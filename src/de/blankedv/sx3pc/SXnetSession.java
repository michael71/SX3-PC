package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.MainUI.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * hanles one session (=1 mobile device)
 */
public class SXnetSession implements Runnable {

    private static int session_counter = 0;  // class variable !

    private int sn; // session number
    private final Socket incoming;
    private PrintWriter out;

    // list of channels which are of interest for this device
    private final int[] sxDataCopy;

    private final int ERROR = INVALID_INT;  // ERROR kept for readability

    /**
     * Constructs a handler.
     *
     * @param sock the incoming socket
     */
    public SXnetSession(Socket sock) {
        incoming = sock;
        sxDataCopy = new int[128];
        sn = session_counter++;
    }

    public void run() {
        try {
            OutputStream outStream = incoming.getOutputStream();
            out = new PrintWriter(outStream, true /* autoFlush */);
            InputStream inStream = incoming.getInputStream();
            Scanner in = new Scanner(inStream);

            Timer timer = new Timer();
            timer.schedule(new Task(), 200, 50);

            sendMessage("SXnet-Server 3.1 - " + sn);  // welcome string

            while (in.hasNextLine()) {
                String msg = in.nextLine().trim().toUpperCase();
                if (msg.length() > 0) {
                    if (DEBUG) {
                        System.out.println("sxnet" + sn + " read: " + msg);
                    }
                    String[] cmds = msg.split(";");  // multiple commands per line possible, separated by semicolon
                    for (String cmd : cmds) {
                        sendMessage(handleCommand(cmd.trim()));  // handleCommand returns "OK" or error msg
                    }
                } else {
                    // ignore empty lines
                    if (DEBUG) {
                        System.out.println("sxnet" + sn + " read empty line");
                    }
                }
                mySleep(10);

            }
            SXnetServerUI.taClients.append("client" + sn + " disconnected " + incoming.getRemoteSocketAddress().toString() + "\n");
        } catch (IOException e) {
            System.out.println("SXnetServerHandler" + sn + " Error: " + e);
        }
        try {
            incoming.close();
        } catch (IOException ex) {
            System.out.println("SXnetServerHandler" + sn + " Error: " + ex);
        }

        System.out.println("Closing SXnetserverHandler" + sn + "\n");
    }

    // handles feedback, if the sxData have been changed on the SX-Bus
    // feedback both for low (<256) addresses == SX-only (+ Lanbahn if mapping exists)
    // and for high "lanbahn" type addresses
    class Task extends TimerTask {

        public void run() {
            checkForChangedSXDataAndSendUpdates();
            mySleep(200);  // send update only every 200msecs
        }
    }

    /**
     * SX Net Protocol (ASCII, all msg terminated with '\n')
     *
     * ---------------------------------------|------------------- 
     * R cc = Read channel cc (0..127) 
     * S cc dd = Set channel cc to Data dd   (instead of 'S' 'SX' is also possible)
     * 
     * channel 127 bit 8 == Track Power
     *
     * for all channels 0 ... 104 (SXMAX_USED) and 127 all
     * changes are transmitted to all connected clients
     * ,
     */
    private String handleCommand(String m) {
        String[] param = m.split("\\s+");  // remove >1 whitespace
        if (param == null) {
            return "ERROR";
        }
        if (param.length < 2) {
            System.out.println("not enough params in msg: " + m);
            return "ERROR";
        }

        switch (param[0]) {
            case "R":
                return createSXFeedbackMessage(param);
            case "S":
            case "SX":
                return setSXMessage(param);
            default:
                return "";

        }

    }

    private String createSXFeedbackMessage(String[] par) {
        if (DEBUG) {
            System.out.println("createSXFeedbackMessage");
        }
        int adr = getSXAddrFromString(par[1]);
        if (adr == INVALID_INT) {
            return "ERROR";
        }

        if (DEBUG) {
            System.out.println(" adr=" + adr);
        }
        if (adr <= 127) {
            return "X " + adr + " " + sxData[adr];
        } else {
            return "";
        }
    }

    private String setSXMessage(String[] par) {
        if (par.length < 3) {
            return "ERROR";
        }
        if (DEBUG) {
            System.out.println("setSXMessage");
        }
        int adr = getSXAddrFromString(par[1]);
        int data = getByteFromString(par[2]);

        if ((adr == INVALID_INT) || (data == INVALID_INT)) {
            return "ERROR";
        }

        if (adr <= 127) {  // SX0  (bus=0)
            sxData[adr] = data;
            sxi.send2SX(adr, data);
            return "X " + adr + " " + sxData[adr];
        } else {
            return ""; // TODO check what is done with emtpy string
        }

    }

    
    private int getBitFromString(String s) {
        // converts String to an integer between 1 and 8 (=SX Bit)
        Integer bit = ERROR;
        try {
            bit = Integer.parseInt(s);
            if ((bit < 1) || (bit > 8)) {
                bit = ERROR;
            }
        } catch (Exception e) {
            bit = ERROR;
        }
        return bit;
    }

    private int getByteFromString(String s) {
        // converts String to integer between 0 and 255 
        //    (= range of SX Data and of Lanbahn data values)
        Integer data;
        try {
            data = Integer.parseInt(s);
            if ((data >= 0) && (data <= 255)) {  // 1 byte
                return data;
            }
        } catch (Exception e) {
            //
        }
        return ERROR;
    }

    /**
     * extract the selectrix address from a string, only valid addresses
     * 0...111,127 and 128..139,255 are allowed, else "INVALID_INT" is returned
     *
     * @param s
     * @return addr (or INVALID_INT)
     */
    int getSXAddrFromString(String s) {
        if (DEBUG) {
            System.out.println("get SXAddr from " + s);
        }
        Integer channel = ERROR;
        try {
            channel = Integer.parseInt(s);
            if (isValidSXAddress(channel)) {
                // SX channel polling einschalten, wenn nicht schon passiert
                if (!sx.getpList().contains(channel)) {
                    sx.addToPlist(channel);
                }
                return channel;
            } else {
                return ERROR;
            }

        } catch (Exception e) {
            System.out.println("ERROR: number conversion error input=" + s);
            return ERROR;
        }
    }

    /**
     * is the address a valid SX0 or SX1 address ?
     *
     * @param address
     * @return true or false
     */
    private boolean isValidSXAddress(int a) {

        if (((a >= SXMIN) && (a <= SXMAX)) || (a == SXPOWER)) {
            //if (DEBUG) System.out.println("isValidSXAddress? "+a + " true (SX0");
            return true;  // 0..111 or 127
        }

        //if (DEBUG) System.out.println("isValidSXAddress? "+a + " false");
        return false;
    }

    private void sendMessage(String res) {
        out.println(res);
        out.flush();
        if (DEBUG) {
            System.out.println("sxnet" + sn + " send: " + res);
        }
    }

    private void mySleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Logger.getLogger(SXnetSession.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * if channel data changed, send update to clients
     *
     * @param bus (0 or 1)
     * @param sxaddr (valid sxaddr)
     */
    private void sendSXUpdates(int sxAddr) {

        sxDataCopy[sxAddr] = sxData[sxAddr];

        String msg = "X " + sxAddr + " " + sxDataCopy[sxAddr];  // SX Feedback Message
        if (DEBUG) {
            System.out.println("sent: " + msg);
        }

        sendMessage(msg);  // send all messages, separated with ";"

    }

    /**
     * check for changed sxData and send update in case of change
     */
    private void checkForChangedSXDataAndSendUpdates() {

        // power channel
        if (sxData[127] != sxDataCopy[127]) {
            sendSXUpdates(127);
        }
        // other channels

        for (int ch = 0; ch < SXMAX; ch++) {
            if (sxData[ch] != sxDataCopy[ch]) {
                // channel data changed, send update to mobile device
                sendSXUpdates(ch);
            }

        }

    }
}
