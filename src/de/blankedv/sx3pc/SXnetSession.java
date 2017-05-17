package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.InterfaceUI.DEBUG;
import static de.blankedv.sx3pc.InterfaceUI.INVALID_INT;
import static de.blankedv.sx3pc.InterfaceUI.lanbahnData;
import static de.blankedv.sx3pc.InterfaceUI.sx;
import static de.blankedv.sx3pc.InterfaceUI.sxData;
import static de.blankedv.sx3pc.InterfaceUI.sxi;

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
import static de.blankedv.sx3pc.InterfaceUI.useSX1forControl;
import static de.blankedv.sx3pc.InterfaceUI.LBMAX;
import static de.blankedv.sx3pc.InterfaceUI.lanbahnData;
import static de.blankedv.sx3pc.InterfaceUI.sxData;
import static de.blankedv.sx3pc.InterfaceUI.sxbusControl;
import static de.blankedv.sx3pc.InterfaceUI.sxi;

/**
 * hanles one session (=1 mobile device)
 */
public class SXnetSession implements Runnable {

    private final Socket incoming;
    private static int session_counter = 0;
    private final int session_id;
    // list of channels which are of interest for this device
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
     *
     * addresses > 128 are "lanbahn messages", which can have a mapping to SX
     * addresses, see "UtilityMapping.java"
     */
    private String handleCommand(String m) {
        String[] param = m.split("\\s+");  // one or more whitespace

        if (param[0].equals("R") || param[0].equals("READ")) {  // read a channel
            if (param.length < 2) {
                return "ERROR";
            }
            int adr = getChannelFromString(param[1]);
            if (adr == ERROR) {
                return "ERROR";
            }
            if (isSXAddress(adr)) {
                String res = "";
                if (adr > 127) {
                    if (adr < 256) {
                        res = "X " + adr + " " + sxData[adr - 128][1];
                    } else {
                        res = "";
                    }
                } else {
                    res = "X " + adr + " " + sxData[adr][0];
                }
                return res;
            } else {
                // lanbahn address range
                String res = "";
                SXAddrAndBits sx = UtilityMapping.getSX(adr);
                if (sx.sxAddr == INVALID_INT) {
                    // pure lanbahn or simulation
                    if (!lanbahnData.containsKey(adr)) {
                        // initialize to "0" (=start simulation and init to "0")
                        // if not already exists
                        lanbahnData.put(adr, 0);
                    }
                    // send lanbahnData, when already set
                    res = "X " + adr + " " + lanbahnData.get(adr);

                } else {
                    // selectrix channel range, get value from SX data array mapping
                    int dOut = INVALID_INT;
                    int d = sxData[sx.sxAddr][sxbusControl];
                    // check mapping for the following bit also
                    switch (sx.nbit) {
                        case 1:
                            dOut = (d >> (sx.bit - 1)) & 0x01;  // sxBit = 1...8
                            break;
                        case 2:
                            dOut = (d >> (sx.bit - 1)) & 0x03;  // two consecutive bits
                            break;
                    }
                    if (dOut != INVALID_INT) {
                        res = "X " + adr + " " + dOut;
                        return res;
                    } else {
                        System.out.println("ERROR, could not get SX Value");
                        return "ERROR";
                    }
                }

            }

        } else if (param[0].equals("B")) {  // set/change an SX Bit
            if (param.length < 3) {
                return "ERROR";
            }
            int adr = getChannelFromString(param[1]);
            int bit = getBitFromString(param[2]);
            if ((adr != ERROR) && (bit != ERROR)) {
                sxi.send2SXBussesBit(adr, bit, 1);
                return "X " + adr + " " + sxData[adr][sxbusControl];
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
                sxi.send2SXBussesBit(adr, bit, 0);
                return "X " + adr + " " + sxData[adr][sxbusControl];
            } else {
                return "ERROR";
            }
        } else if (param[0].equals("S") || param[0].equals("SET")) {  // set a Channel to a new value
            if (param.length < 3) {
                return "ERROR";
            }
            int adr = getChannelFromString(param[1]);
            int data = getDataFromString(param[2]);
            if ((adr != ERROR) && (data != ERROR)) {
                if (isSXAddress(adr)) {
                    sxi.send2SXBusses(adr, data);
                    return "X " + adr + " " + data;
                } else {
                    // set lanbahn channel

                    SXAddrAndBits sx = UtilityMapping.getSX(adr);
                    if (DEBUG) {
                        System.out.println("lb-in: lbaddr=" + adr + " val=" + data);
                        System.out.println("sx: " + sx.toString());
                    }
                    if (sx.sxAddr == INVALID_INT) {
                        // pure lanbahn address range
                        lanbahnData.put(adr, data);
                    } else {
                        // selectrix channel range   

                        try_set_sx_accessory(sx, data);

                    }
                    return "X " + adr + " " + data;
                }
            } else {
                return "ERROR";
            }
        } else if (param[0].equals("RT")) {  // route message
            // just reply XRT with the same adr and data
            if (param.length < 3) {
                return "ERROR";
            }
            int adr = getChannelFromString(param[1]);
            int data = getDataFromString(param[2]);
            if ((adr != ERROR) && (data != ERROR)) {
                return "XRT " + adr + " " + data;
            } else {
                return "ERROR";
            }
        }
        return "ERROR";

    }

    private void try_set_sx_accessory(SXAddrAndBits sx, int data) {
        if (!sxi.isConnected()) {
            System.out.println("could not set SX, interface not connected");
            return;
        }
        if ((sx.bit >= 1) && (sx.bit <= 8) && (sx.nbit <= 4) && (sx.nbit >= 1)) {
            int d = sxData[sx.sxAddr][sxbusControl];
            switch (sx.nbit) {
                case 1:
                    if (data > 1) {
                        System.out.println("could not set SX, data >1 (nbit = 1)");
                        return;
                    }
                    d = SXUtils.bitOperation(d, sx.bit, (data & 0x01));
                    break;
                case 2:
                    if (data > 3) {
                        System.out.println("could not set SX, data >3 (nbit = 2)");
                        return;
                    }
                    d = SXUtils.bitOperation(d, sx.bit, (data & 0x01));
                    d = SXUtils.bitOperation(d, sx.bit + 1, (data & 0x02));
                    break;
                case 3:
                    if (data > 7) {
                        System.out.println("could not set SX, data >7 (nbit = 3)");
                        return;
                    }
                    d = SXUtils.bitOperation(d, sx.bit, (data & 0x01));
                    d = SXUtils.bitOperation(d, sx.bit + 1, (data & 0x02));
                    d = SXUtils.bitOperation(d, sx.bit + 2, (data & 0x04));
                    break;
                case 4:
                    if (data > 15) {
                        System.out.println("could not set SX, data >15 (nbit = 4)");
                        return;
                    }
                    d = SXUtils.bitOperation(d, sx.bit, (data & 0x01));
                    d = SXUtils.bitOperation(d, sx.bit + 1, (data & 0x02));
                    d = SXUtils.bitOperation(d, sx.bit + 2, (data & 0x04));
                    d = SXUtils.bitOperation(d, sx.bit + 2, (data & 0x08));
                    break;

            }
            sxi.sendAccessory(sx.sxAddr, d);  // set changed data value
            if (DEBUG) {
                System.out.println("setting sx-adr=" + " val=" + d);
            }
        } else {
            if (DEBUG) {
                System.out.println("could not set sx-adr=" + sx.sxAddr + " bit=" + sx.bit);
            }
        }
    }

    private boolean isSXAddress(int a) {
        int maxchan = 127;
        if (useSX1forControl) {
            maxchan = maxchan + 128;
        }
        if ((a >= 0) && (a <= maxchan)) {
            // SX channel
            return true;
        } else {
            return false;
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
        if (useSX1forControl) {
            maxchan = maxchan + 128;
        }
        Integer channel = ERROR;
        try {
            channel = Integer.parseInt(s);
            if ((channel >= 0) && (channel <= maxchan)) {
                // SX channel
                // polling einschalten, wenn nicht schon passiert
                if (!sx.getpList().contains(channel)) {
                    sx.addToPlist(channel);
                }
                return channel;
            } else if (channel <= LBMAX) {
                // lanbahn channel
                return channel;
            }
        } catch (Exception e) {
            // error is default, see above
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

            for (int bus = 0; bus < 2; bus++) {
                if (sxData[127][bus] != sxDataCopy[127][bus]) {
                    // POWER ON/OFF state changed, send update to mobile device
                    sxDataCopy[127][bus] = sxData[127][bus];
                    System.out.println("X 127 " + sxDataCopy[127][bus]);
                    sendMessage("X 127 " + sxDataCopy[127][bus]);
                }
                for (int i = 0; i < 112; i++) {
                    if (sxData[i][bus] != sxDataCopy[i][bus]) {
                        // channel data changed, send update to mobile device
                        sxDataCopy[i][bus] = sxData[i][bus];
                        System.out.println("X " + i + " " + sxDataCopy[i][bus]);
                        sendMessage("X " + i + " " + sxDataCopy[i][bus]);
                    }
                }
            }
            try {
                Thread.sleep(300);  // send update only every 300msecs
            } catch (InterruptedException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }
}
