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

    /**
     * Thread receives messages from one mobile device
     *
     */
    public void run() {
        try {
            OutputStream outStream = incoming.getOutputStream();
            out = new PrintWriter(outStream, true /* autoFlush */);
            InputStream inStream = incoming.getInputStream();
            Scanner in = new Scanner(inStream);

            Timer sendUpdatesTimer = new Timer();
            sendUpdatesTimer.schedule(new SendUpdatesTask(), 200, 50);

            sendMessage("SXnet-Server 3.1 - " + sn);  // welcome string

            while (in.hasNextLine()) {
                String msg = in.nextLine().trim().toUpperCase();
                if (msg.length() > 0) {
                    if (DEBUG) {
                        System.out.println("sxnet" + sn + " read: " + msg);
                    }
                    String[] cmds = msg.split(";");  // multiple commands per line possible, separated by semicolon
                    for (String cmd : cmds) {
                        sendMessage(handleCommand(cmd.trim()));
                        // sends feedback message  X 'addr' 'data' (or INVALID_INT) back to mobile device
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
    class SendUpdatesTask extends TimerTask {

        public void run() {
            checkForChangedSXDataAndSendUpdates();
            mySleep(200);  // send update only every 200msecs
        }
    }

    /**
     * SX Net Protocol (ASCII, all msg terminated with '\n')
     *
     * sent by mobile device -> SX3-PC sends back:
     * ---------------------------------------|------------------- R cc = Read
     * channel cc (0..127) -> returns "X cc dd" S cc.b dd = Set channel cc bit b
     * to Data dd (0 or 1) -> returns "X cc dd" SX cc dd = Set channgel cc to
     * byte dd -> returns "X cc dd"
     *
     * channel 127 bit 8 == Track Power
     *
     * for all channels 0 ... 104 (SXMAX_USED) and 127 all changes are
     * transmitted to all connected clients ,
     */
    private String handleCommand(String m) {
        String[] param = m.split("\\s+");  // remove >1 whitespace
        if (param == null) {
            System.out.println("irregular msg: " + m);
        }
        if (param.length < 2) {
            System.out.println("not enough params in msg: " + m);
        }

        String result = "";
        switch (param[0]) {
            case "R":    // returns byte
                result = readSXByteMessage(param);
                break;
            case "S":    // single bit set / unset
                setSXBitMessage(param);
                break;
            case "SX":   // complete byte set (for loco typically)
                setSXByteMessage(param);
                break;
            default:
        }
        return result;

    }

    private String readSXByteMessage(String[] par) {
        if (DEBUG) {
            System.out.println("createSXFeedbackMessage");
        }
        int adr = getSXAddrFromString(par[1]);
        if (adr == INVALID_INT) {
            System.out.println("addr in msg invalid");
            return "";
        }
        return "X " + adr + " " + sxData[adr];
    }

    private void setSXByteMessage(String[] par) {
        if (par.length < 3) {
            return;
        }
        if (DEBUG) {
            System.out.println("setSXByteMessage");
        }
        int adr = getSXAddrFromString(par[1]);
        int data = getByteFromString(par[2]);

        if ((adr == INVALID_INT) || (data == INVALID_INT)) {
            return;
        }

        SXUtils.setSxData(adr, data);
    }

    
    private void setSXBitMessage(String[] par) {
        if (par.length < 3) {
            return;
        }
        if (DEBUG) {
            System.out.println("setSXBitMessage");
        }
        SxAbit sxb = getSXAbitFromString(par[1]);

        int data = getByteFromString(par[2]);

        if ((sxb.addr == INVALID_INT) || (data == INVALID_INT)) {
            return;
        }
        if (data == 1) {
            //set bit
            SXUtils.setBitSxData(sxb.addr, sxb.bit);
        } else if (data == 0) {
            // clear bit
            SXUtils.clearBitSxData(sxb.addr, sxb.bit);
        }

    }

    /* NOT USED private int getBitFromString(String s) {
        // converts String to an integer between 1 and 8 (=SX Bit)
        try {
            int bit = Integer.parseInt(s);
            if ((bit >= 1) || (bit <= 8)) {
                return bit;
            }
        } catch (NumberFormatException e) {
        }
        return INVALID_INT;
    } */
    private int getByteFromString(String s) {
        // converts String to integer between 0 and 255 
        //    (= range of SX Data and of Lanbahn data values)
        try {
            int data = Integer.parseInt(s);
            if ((data >= 0) && (data <= 255)) {  // 1 byte
                return data;
            }
        } catch (NumberFormatException e) {
            //
        }
        return INVALID_INT;
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
        try {
            int channel = Integer.parseInt(s);
            if (SXUtils.isValidSXAddress(channel)) {
                // SX channel polling einschalten, wenn nicht schon passiert
                if (!sx.getpList().contains(channel)) {
                    sx.addToPlist(channel);
                }
                return channel;
            } else {
                return INVALID_INT;
            }

        } catch (Exception e) {
            System.out.println("ERROR: number conversion error input=" + s);
            return INVALID_INT;
        }
    }

    /**
     * extract the selectrix address from a string and the SX bit only valid
     * addresses 0...111,127 are allowed and valid bit (1..8)
     *
     * @param s
     * @return SxAbit (addr,bit)
     */
    SxAbit getSXAbitFromString(String s) {
        if (DEBUG) {
            System.out.println("get SXAbit from " + s);
        }
        String[] sxab = s.split("\\.");  // regular expression! not character
        if (sxab.length != 2) {
            if (DEBUG) {
                System.out.println("length != 2 - l=" + sxab.length);
            }
            return new SxAbit(INVALID_INT, INVALID_INT);
        }
        try {
            int channel = Integer.parseInt(sxab[0]);
            if (SXUtils.isValidSXAddress(channel)) {
                // SX channel polling einschalten, wenn nicht schon passiert
                if (!sx.getpList().contains(channel)) {
                    sx.addToPlist(channel);
                }
                int bit = Integer.parseInt(sxab[1]);
                if (SXUtils.isValidSXBit(bit)) {
                    if (DEBUG) {
                        System.out.println("valid, a=" + channel + " bit=" + bit);
                    }
                    return new SxAbit(channel, bit);
                }
            }

        } catch (Exception e) {
            System.out.println("ERROR: number conversion error input=" + s);

        }
        return new SxAbit(INVALID_INT, INVALID_INT);
    }

    private void sendMessage(String res) {
        if (res.isEmpty()) {
            return;
        }

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
