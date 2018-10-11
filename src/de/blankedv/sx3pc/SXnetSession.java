package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.MainUI.*;
import de.blankedv.timetable.CompRoute;
import de.blankedv.timetable.LbUtils;
import de.blankedv.timetable.PanelElement;
import de.blankedv.timetable.Route;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

/**
 * hanles one session (=1 mobile device)
 */
public class SXnetSession implements Runnable {

    private static int session_counter = 0;  // class variable !
    private String lastRes = "";
    private long lastSent = 0;

    private int sn; // session number
    private final Socket incoming;
    private PrintWriter out;

    // list of channels which are of interest for this device
    private final int[] sxDataCopy;
    private int lastConnected = INVALID_INT;
    private final ConcurrentHashMap<Integer, Integer> oldPEStateCopy = new ConcurrentHashMap<>(500);

    private int globalPowerCopy = INVALID_INT;
    private int centralRoutingCopy = INVALID_INT;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

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

    public void stop() {
        running.set(false);
        worker.interrupt();
    }

    /**
     * Thread receives messages from one mobile device
     *
     */
    public void run() {
        running.set(true);
        worker = Thread.currentThread();
        try {
            OutputStream outStream = incoming.getOutputStream();
            out = new PrintWriter(outStream, true /* autoFlush */);
            InputStream inStream = incoming.getInputStream();
            Scanner in = new Scanner(inStream);
            long lastCommand = System.currentTimeMillis();

            Timer sendUpdatesTimer = new Timer();
            sendUpdatesTimer.schedule(new SendUpdatesTask(), 1000, 100);

            sendMessage(S_XNET_SERVER_REV + " client" + sn);  // welcome string

            while (running.get() && in.hasNextLine()) {
                String msg = in.nextLine().trim().toUpperCase();
                if (msg.length() > 0) {
                    if (DEBUG) {
                        System.out.println("sxnet" + sn + " read: " + msg);
                    }
                    String[] cmds = msg.split(";");  // multiple commands per line possible, separated by semicolon
                    for (String cmd : cmds) {
                        handleCommand(cmd.trim());
                        // sends feedback message  XL 'addr' 'data' (or INVALID_INT) back to mobile device
                    }
                    lastCommand = System.currentTimeMillis();
                } else {
                    // ignore empty lines
                    if (DEBUG) {
                        System.out.println("sxnet" + sn + " read empty line");
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println(
                            "client" + sn + " Thread was interrupted");
                }

            }
            SXnetServerUI.taClients.append("client" + sn + " disconnected" + incoming.getRemoteSocketAddress().toString() + "\n");
            sendUpdatesTimer.cancel();

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
            checkForLanbahnChangesAndSendUpdates();
        }
    }

    /**
     * SX Net Protocol (ASCII, all msg terminated with '\n') REV JULY 2018 sent
     * by mobile device -> SX3-PC sends back:
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
    private void handleCommand(String m) {
        String[] param = m.split("\\s+");  // remove >1 whitespace
        if (param == null) {
            System.out.println("irregular msg: " + m);
        }
        if (param[0].equals("READPOWER")) {
            String res = readPower();  // no parameters
            sendMessage(res);
            return;
        }
        if (param.length < 2) {
            System.out.println("not enough params in msg: " + m);
        }

        String result = "";
        switch (param[0]) {
            case "SETPOWER":
                setPower(param);
                break;
            case "SETLOCO":   // complete byte set (for loco typically)
                setLocoMessage(param);
                break;
            case "READLOCO":    // returns byte
                result = readLocoMessage(param);
                break;
            case "S":    // SX Byte set
                setSXByteMessage(param);
                break;
            case "R":    // read sx value
                result = readSXByteMessage(param);
                break;
            case "REQ":
                result = requestRouteMessage(param);
                break;
            case "SET": //TODO, for addresses > 1000 (lanbahn sim./routes)
                setLanbahnMessage(param);
                break;
            case "READ": //TODO, for addresses > 1000 (lanbahn sim./routes)
                result = createLanbahnFeedbackMessage(param);
                break;
            case "QUIT": //terminate this client thread
                stop();
                break;
            default:
        }
        sendMessage(result);

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

    private String readLocoMessage(String[] par) {
        if (DEBUG) {
            System.out.println("readLocoMessage");
        }
        int adr = getSXAddrFromString(par[1]);
        if (adr == INVALID_INT) {
            System.out.println("addr in msg invalid");
            return "";
        }
        if (!locoAddresses.contains(adr)) {
            locoAddresses.add(adr);
        }
        return "XLOCO " + adr + " " + sxData[adr];
    }

    private String requestRouteMessage(String[] par) {
        if (DEBUG) {
            System.out.println("requestRouteMessage");
        }
        if (par.length <= 2) {
            if (DEBUG) {
                System.out.println("par.length <=2");
            }
            return "ERROR";
        }

        // parse string
        int lbAddr = getLanbahnAddrFromString(par[1]);
        int lbdata = getLanbahnDataFromString(par[2]);   // can only be 1= set and 0=clear
        if ((lbAddr == INVALID_INT) || ((lbdata != 0) && (lbdata != 1))) {
            if (DEBUG) {
                System.out.println("LB-addr or -data invalid");
            }
            return "ERROR";
        }

        // check whether there is a route with this address(=adr)
        Route r = Route.getFromAddress(lbAddr);
        if (r != null) {
            boolean res = r.set();
            if (res) {              
                return "XL " + lbAddr + " " + r.getState();  // success
            } else {
                if (DEBUG) {
                    System.out.println("route invalid");
                }
                return "ROUTE_INVALID";
            }
        }

        // check whether there is a compound route with this address(=adr)
        CompRoute cr = CompRoute.getFromAddress(lbAddr);
        if (cr != null) {
            boolean res = cr.set();
            if (res) {
                return "XL " + lbAddr + " " + cr.getState();  // success
            } else {
                if (DEBUG) {
                    System.out.println("comp route invalid");
                }
                return "ROUTE_INVALID";
            }
        }
        if (DEBUG) {
            System.out.println("no route or compound found");
        }
        return "ERROR";

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
        SXUtils.setSxData(adr, data);  // synchronized
    }

    private void setLocoMessage(String[] par) {
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

        SXUtils.setSxData(adr, data);  // synchronized
    }

    private String setPower(String[] par) {
        if (DEBUG) {
            System.out.println("setPowerMessage");
        }
        int value = getByteFromString(par[1]);
        switch (value) {
            case 1:
                SXUtils.setSxData(127, 128);  // synchronized
                return "XPOWER 1";
            case 0:
                SXUtils.setSxData(127, 0);  // synchronized
                return "XPOWER 0";
            default:
                return "";   // invalid value
        }

    }

    private String readPower() {
        if (DEBUG) {
            System.out.println("readPowerMessage");
        }
        if (sxData[127] != 0) {
            return "XPOWER 1";
        } else {
            return "XPOWER 0";

        }

    }

    /* private void setSXBitMessage(String[] par) {
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

    } */
    /**
     * when setting the data for a lanbahn address, there are 3 possible
     * scenarios: A) it is within the SX address range and only has a single bit
     * of data B) it is within the SX address range, but has 2 bits. The first
     * bit will be a pure SX-bit, the second bit can either be an SX-bit or a
     * virtual lanbahn address C) it is outside the SX address range => it is a
     * virtual lanbahn address
     *
     * @param par
     * @return
     */
    private String setLanbahnMessage(String[] par) {
        if (DEBUG) {
            // System.out.println("setLanbahnMessage");
        }

        if (par.length <= 2) {
            return "ERROR";
        }
        int lbadr = getLanbahnAddrFromString(par[1]);
        int lbdata = getLanbahnDataFromString(par[2]);
        if ((lbadr == INVALID_INT) || (lbdata == INVALID_INT)) {
            return "ERROR";
        } else {
            // check if we have a matching PanelElement
            PanelElement pe = PanelElement.getByAddress(lbadr);
            if (pe != null) {
                pe.setState(lbdata);
                // send lanbahnData
                return "XL " + lbadr + " " + pe.getState();
            }
            return "ERROR";
        }
    }

    private String createLanbahnFeedbackMessage(String[] par) {
        if (DEBUG) {
            System.out.println("createLanbahnFeedbackMessage");
        }
        int lbAddr = getLanbahnAddrFromString(par[1]);
        if (lbAddr == ERROR) {
            return "ERROR";
        } else {
            PanelElement pe = PanelElement.getByAddress(lbAddr);
            if (pe != null) {
                    // send lanbahnData
                return "XL " + lbAddr + " " + pe.getState();
            }
            return "ERROR";
        }
    }

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

    private int getLanbahnDataFromString(String s) {
        // converts String to integer between 0 and 3
        //    (= range Lanbahn data values)
        Integer data;
        try {
            data = Integer.parseInt(s);
            if ((data >= LBDATAMIN) && (data <= LBDATAMAX)) {
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
        try {
            int channel = Integer.parseInt(s);
            if (SXUtils.isValidSXAddress(channel)) {
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

    /**
     * parse String to extract a lanbahn address
     *
     * @param s
     * @return lbaddr (or INVALID_INT)
     */
    int getLanbahnAddrFromString(String s) {
        if (DEBUG) {
            System.out.println("getLanbahnAddrFromString s=" + s);
        }
        Integer lbAddr;
        try {
            lbAddr = Integer.parseInt(s);
            if ((lbAddr >= LBMIN) && (lbAddr <= LBMAX)) {
                return lbAddr;
                // OK, valid lanbahn channel
            } else {
                System.out.println("ERROR: lbAddr=" + lbAddr + " not valid");
                return ERROR;
            }
        } catch (Exception e) {
            System.out.println("ERROR: number conversion error input=" + s);
            return ERROR;
        }
    }

    public void sendMessage(String res) {

        // don't send duplicate messages within 1 second
        if (res.isEmpty() || (res.equals(lastRes) && (System.currentTimeMillis() - lastSent < 1000))) {
            return;
        }

        // store for later use
        lastRes = res;
        lastSent = System.currentTimeMillis();

        out.println(res);
        //out.flush(); autoflush is set to true
        if (DEBUG) {
            System.out.println("sxnet" + sn + " send: " + res);
        }
    }

    /**
     * if channel data changed, send update to clients
     *
     * @param lbaddr
     */
    private void sendSXUpdates(int lbAddr) {

        int sxAddr = lbAddr / 10;

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
        StringBuilder msg = new StringBuilder();
        boolean first = true;

        // report change in power channel
        if (sxData[127] != sxDataCopy[127]) {
            sxDataCopy[127] = sxData[127];
            if (sxDataCopy[127] != 0) {
                msg.append("XPOWER 1");
            } else {
                msg.append("XPOWER 0");
            }
            first = false;
        }

        // report change in connect status
        if ((lastConnected == INVALID_INT) || (sxi.connState() != lastConnected)) {
            lastConnected = sxi.connState();
            if (!first) {
                msg.append(";");
            }
            msg.append("XCONN ");
            msg.append(lastConnected); // 1 or 0
            first = false;
        }

        // report changes in other channels
        for (int ch = 0; ch < SXMAX; ch++) {
            if (sxData[ch] != sxDataCopy[ch]) {
                sxDataCopy[ch] = sxData[ch];
                // channel data changed, send update to mobile device 
                if (!first) {
                    msg.append(";");
                }

                if (locoAddresses.contains(ch)) {
                    msg.append("XLOCO ");
                } else {
                    msg.append("X ");
                }
                msg.append(ch).append(" ").append(sxDataCopy[ch]);  // SX Feedback Message
                first = false;

                if (msg.length() > 60) {
                    sendMessage(msg.toString());
                    msg.setLength(0);  // =delete content
                    first = true;
                }
            }

        }
        sendMessage(msg.toString());  // send all messages, separated with ";"
    }

    /**
     * check for changed (exclusiv) lanbahn data and send update in case of
     * change
     *
     */
    private void checkForLanbahnChangesAndSendUpdates() {
        StringBuilder msg = new StringBuilder();
        int globalPower = sxData[127] & 0x80;
        if ((globalPowerCopy != globalPower) && (globalPower != INVALID_INT)) {
            // power state has changed
            globalPowerCopy = globalPower;
            msg.append("XPOWER ");
            msg.append(globalPower);
        }

        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        int centralR = 0;
        if (prefs.getBoolean("centralRouting", false)) {
            centralR = 1;
        }
        if (centralRoutingCopy != centralR) {
            // power state has changed
            centralRoutingCopy = centralR;
            if (msg.length() != 0) {
                msg.append(";");
            }
            msg.append("ROUTING ");
            msg.append(centralRoutingCopy);
        }
        
        TreeMap<Integer,Integer> actData = peStateCopy();
        for (Map.Entry<Integer, Integer> e : actData.entrySet()) {
            Integer key = e.getKey();
            Integer value = e.getValue();
            if (!oldPEStateCopy.containsKey(key) || (!Objects.equals(oldPEStateCopy.get(key), actData.get(key)))) {  // null-safe '=='
                // value is new or has changed
                oldPEStateCopy.put(key, value);
                if (msg.length() != 0) {
                    msg.append(";");
                }
                msg.append("XL ").append(key).append(" ").append(value);
                if (msg.length() > 60) {
                    sendMessage(msg.toString());
                    msg.setLength(0);  // =delete content
                }
            }
        }
        if (msg.length() > 0) {
            sendMessage(msg.toString());
        }
    }
    
     private TreeMap<Integer,Integer> peStateCopy() {
        TreeMap<Integer,Integer> hm = new TreeMap<>();
        panelElements.forEach((pe) -> hm.put(pe.getAdr(),pe.getState()));
        return hm;
    }
}
