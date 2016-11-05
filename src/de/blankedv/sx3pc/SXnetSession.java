package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.InterfaceUI.DEBUG;
import static de.blankedv.sx3pc.InterfaceUI.sx;
import static de.blankedv.sx3pc.InterfaceUI.sxData;
import static de.blankedv.sx3pc.InterfaceUI.sxi;
import static de.blankedv.sx3pc.InterfaceUI.twoBusses;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

/**
 * hanles one session (=1 mobile device)
 */
public class SXnetSession implements Runnable {

    private final Socket incoming;
    private static int session_counter = 0;
    private final int session_id;
    // list of channels which are of interest for this device
    private final List<Integer> sxList;
    private final int[][] sxDataCopy;
    private int timeElapsed;
    private final long timeFB;
    private final boolean lastPower;
    protected PrintWriter out;
    private static final int ERROR = 9999;

    /**
     * Constructs a handler.
     *
     * @param i the incoming socket
     */
    public SXnetSession(Socket i) {
        incoming = i;
        session_id = ++session_counter;
        sxDataCopy = new int[128][2];
        sxList = new ArrayList<Integer>();
        timeFB = 0;
        lastPower = false;
    }

    public void run() {
        try {
            OutputStream outStream = incoming.getOutputStream();
            out = new PrintWriter(outStream, true /* autoFlush */);
            InputStream inStream = incoming.getInputStream();
            Scanner in = new Scanner(inStream);

            Timer timer = new Timer();
            timer.schedule(new Task(), 1000, 1000);

            sendMessage("SXnet-Server 0.1");  // welcome string

            while (in.hasNextLine()) {
                String msg = in.nextLine().trim().toUpperCase();
                if (msg.length() > 0) {
                    if (DEBUG) {
                        System.out.println("sxnet read: " + msg);
                    }

                    sendMessage(handleCommand(msg));  // handleCommand returns "OK" or error msg

                } else {
                    // ignore empty lines
                    if (DEBUG) {
                        System.out.println("sxnet read empty line");
                    }
                }

            }
            SXnetServerUI.taClients.append("client disconnected " + incoming.getRemoteSocketAddress().toString() + "\n");
        } catch (IOException e) {
            System.out.println("SXnetServerHandler Error: " + e);
        }
        try {
            incoming.close();
        } catch (IOException ex) {
            System.out.println("SXnetServerHandler Error: " + ex);
        }

        System.out.println("Closing SXnetserverHandler\n");
    }

    /**
     * SX Net Protocol (all msg terminated with CR)
     *
     * client sends | SXnetServer Response
     * ---------------------------------------|------------------- R cc = Read
     * channel cc (0..127) | "X" cc dd B cc b = SetBit Ch. cc Bit b (1..8) |
     * "OK" (and later, when changed in CS: X cc dd ) C cc b = Clear Ch cc Bit b
     * (1..8) | "OK" (and later, when changed in CS: X cc dd ) S cc dd = set
     * channel cc Data dd (<256)| "OK" (and later, when changed in CS: X cc dd )
     * DSDF 89sf (i.e. garbage) | "ERROR"
     *
     * channel 127 bit 8 == Track Power
     *
     * for a list of channels (which the client has set or read in the past) all
     * changes are transmitted back to the client
     */
    private String handleCommand(String m) {
        String[] param = m.split("\\s+");  // one or more whitespace

        if (param[0].equals("R")) {  // read an SX channel
            if (param.length < 2) {
                return "ERROR";
            }
            int adr = getChannelFromString(param[1]);
            if (adr != ERROR) {
                checkSXlist(adr);
                String res = "";
                if (adr > 127) {
                    res = "X " + adr + " " + sxData[adr][1];
                } else {
                    res = "X " + adr + " " + sxData[adr][0];
                }
                return res;
            } else {
                return "ERROR";
            }

        } else if (param[0].equals("B")) {  // set/change an SX Bit
            if (param.length < 3) {
                return "ERROR";
            }
            int adr = getChannelFromString(param[1]);
            int bit = getBitFromString(param[2]);
            if ((adr != ERROR) && (bit != ERROR)) {
                checkSXlist(adr);
                sxi.send2SXBussesBit(adr, bit, 1);
                return "OK";
            } else {
                return "ERROR";
            }
        } else if (param[0].equals("C")) {  // clear an SX Bit
            if (param.length < 3) {
                return "ERROR";
            }
            int adr = getChannelFromString(param[1]);
            int bit = getBitFromString(param[2]);
            if ((adr != ERROR) && (bit != ERROR)) {
                checkSXlist(adr);
                sxi.send2SXBussesBit(adr, bit, 0);
                return "OK";
            } else {
                return "ERROR";
            }
        } else if (param[0].equals("S")) {  // set an SX Channel to a new value
            if (param.length < 3) {
                return "ERROR";
            }
            int adr = getChannelFromString(param[1]);
            int data = getDataFromString(param[2]);
            if ((adr != ERROR) && (data != ERROR)) {
                checkSXlist(adr);
                sxi.send2SXBusses(adr, data);
                return "OK";
            } else {
                return "ERROR";
            }
        }
        return "ERROR";

    }

    private void checkSXlist(int adr) {
        if (!sxList.contains(adr)) {
            sxList.add(adr);
        }

    }

    private int getBitFromString(String s) {
        // converts String to aan integer between 1 and 8 (=SX Bit)
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

    private int getDataFromString(String s) {
        // converts String to integer between 0 and 255 (=SX Data)
        Integer data = ERROR;
        try {
            data = Integer.parseInt(s);
            if ((data < 0) || (data > 255)) {
                data = ERROR;
            }
        } catch (Exception e) {
            data = ERROR;
        }
        return data;
    }

    private String sendChannel(Integer channel) {
        // build string to send sxChannel info back
        // TODO
        if (channel == ERROR) {
            return "ERROR";
        } else {
            return "C " + channel + " " + 64;
        }
    }

    int getChannelFromString(String s) {
        int maxchan = 127;
        if (twoBusses) {
            maxchan = maxchan + 128;
        }
        Integer channel = ERROR;
        try {
            channel = Integer.parseInt(s);
            if ((channel >= 0) && (channel <= maxchan)) {
                // polling einschalten, wenn nicht schon passiert
                if (!sx.getpList().contains(channel)) {
                    sx.addToPlist(channel);
                }
                return channel;
            } else {
                channel = ERROR;
            }
        } catch (Exception e) {
        }
        return channel;
    }

    private void sendMessage(String res) {
        out.println(res);
        out.flush();
        if (DEBUG) {
            System.out.println("sxnet send:" + res);
        }
    }

    class Task extends TimerTask {

        public void run() {
            int ch;
            int bus;
            for (int i : sxList) {
                if (i < 127) {
                    // SX0
                    ch = i; 
                    bus = 0;
                } else {   
                    //SX1
                    ch = i -127;
                    bus = 1;
                }
                if (sxData[ch][bus] != sxDataCopy[ch][bus]) {
                    // channel data changed, send update to mobile device
                    sxDataCopy[ch][bus] = sxData[ch][bus];
                    System.out.println("X " + i + " " + sxDataCopy[ch][bus]);
                    sendMessage("X " + i + " " + sxDataCopy[ch][bus]);
                 }

            }
            try {
                Thread.sleep(300);  // send update only every 300msecs
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
