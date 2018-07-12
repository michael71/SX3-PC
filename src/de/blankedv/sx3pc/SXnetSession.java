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
import java.util.concurrent.ConcurrentHashMap;
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
    private final int[][] sxDataCopy;
    private final HashMap<Integer, Integer> lanbahnDataCopy = new HashMap<>(N_LANBAHN);
    private final int ERROR = INVALID_INT;  // ERROR kept for readability

    /**
     * Constructs a handler.
     *
     * @param sock the incoming socket
     */
    public SXnetSession(Socket sock) {
        incoming = sock;
        sxDataCopy = new int[128][2];
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

            sendMessage("SXnet-Server 3.0 - " + sn);  // welcome string

            while (in.hasNextLine()) {
                String msg = in.nextLine().trim().toUpperCase();
                if (msg.length() > 0) {
                    if (DEBUG) {
                        System.out.println("sxnet" + sn + " read: " + msg);
                    }
                    String[] cmds = msg.split(";");
                    for (String cmd : cmds) {
                        sendMessage(handleCommand(cmd.trim()));  // handleCommand returns "OK" or error msg
                    }

                } else {
                    // ignore empty lines
                    if (DEBUG) {
                        System.out.println("sxnet" + sn + " read empty line");
                    }
                }
                mySleep(100);

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
            checkForLanbahnChangesAndSendUpdates();
            mySleep(300);  // send update only every 300msecs
        }
    }

    /**
     * SX Net Protocol (all msg terminated with '\n')
     *
     * client sends | SXnetServer Response
     * ---------------------------------------|------------------- R cc = Read
     * channel cc (0..127) | "X" cc dd B cc b = SetBit Ch. cc Bit b (1..8) |
     * "OK" (and later, when changed in CS: X cc dd ) C cc b = Clear Ch cc Bit b
     * (1..8) | "OK" (and later, when changed in CS: X cc dd ) S cc dd = set
     * channel cc Data dd (<256)| "OK" (and later, when changed in CS: X cc dd )
     * DSDF 89sf (i.e. garbage) | "ERROR" *********** NO LONGER BIT MESSAGES
     * ************ July 2018 ********** *********** protocol 3
     * **************************************
     *
     * channel 127 bit 8 == Track Power
     *
     * for a list of channels (which the client has set or read in the past) all
     * changes are transmitted back to the client
     *
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
            case "READ":
                return createLanbahnFeedbackMessage(param);
            case "S":
            case "SX":
                return setSXMessage(param);
            case "SET":
            case "SL":
                return setLanbahnMessage(param);
            case "LOCO":
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
        if (adr == INVALID_INT) return "ERROR";
        if (DEBUG) {
            System.out.println(" adr="+adr);
        }
        if (adr <= 127) {  // SX0
            return "X " + adr + " " + sxData[adr][0];
        } else  { //SX1
            return "X " + adr + " " + sxData[adr - 128][1];
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

        if ((adr == INVALID_INT) || (data == INVALID_INT)) return "ERROR";
        
        if (adr <= 127) {  // SX0  (bus=0)
            sxData[adr][0] = data;
            sxi.send2SXBusses(adr, data);
            return "X " + adr + " " + sxData[adr][0];
        } else { //SX1 (bus=1)
            sxData[adr - 128][1] = data;
            sxi.send2SXBusses(adr, data);
            return "X " + adr + " " + sxData[adr - 128][1];
        } 
    }

    private String createLanbahnFeedbackMessage(String[] par) {
        if (DEBUG) {
            System.out.println("createLanbahnFeedbackMessage");
        }
        int lbAddr = getLanbahnAddrFromString(par[1]);
        if (lbAddr == ERROR) {
            return "ERROR";
        }

        if (isPureLanbahnAddressRange(lbAddr)) {
            if (!lanbahnData.containsKey(lbAddr)) {
                // initialize to "0" (=start simulation and init to "0")
                // if not already exists
                lanbahnData.put(lbAddr, 0);
            }
            // send lanbahnData, when already set
            return "XL " + lbAddr + " " + lanbahnData.get(lbAddr);
        } else if (isLanbahnOverlapAddressRange(lbAddr)) {
            int sxAddr = lbAddr / 10;
            int sxValue = sxData[sxAddr][sxbusControl];
            int lbValue = getLanbahnValueFromSXControlBus(lbAddr);
            if (lbValue != INVALID_INT) {   // can be invalid for example
                // when requesting lbAddr=80 => sxAddr = 8 and bit = 0 or = 9
                // but sxbits range is 1...8
            return "X " + sxAddr + " " + sxValue
                    + ";XL " + lbAddr + " " + lbValue;
            } else {
                return "ERROR";
            }
        } else {
            return "ERROR";
        }
    }

    private boolean isPureLanbahnAddressRange(int a) {
        if ((a == INVALID_INT) || (a < 0)) {
            return false;
        }

        if ((a > LBMIN_LB) && (a <= LBMAX)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isLanbahnOverlapAddressRange(int a) {
        if ((a == INVALID_INT) || (a < 0) ) {
            return false;
        }
        if (a <= LBMIN_LB) {
            return true;
        } else {
            return false;
        }
    }
    
    private String setLanbahnMessage(String[] par) {
        if (DEBUG) {
            System.out.println("setLanbahnMessage");
        }

        // convert the lanbahn "SET" message to an SX-S Message if in SX address range
        if (par.length <= 2) {
            return "ERROR";
        }
        int lbadr = getLanbahnAddrFromString(par[1]);
        int lbdata = getLanbahnDataFromString(par[2]);
        if ((lbadr == INVALID_INT) || (lbdata == INVALID_INT)) {
            return "ERROR";
        }
        if (DEBUG) {
            System.out.println("LB: lbaddr=" + lbadr + " data=" + lbdata);
        }

        // check whether we are in an SX or lanbahn address range
        if (isPureLanbahnAddressRange(lbadr)) {
            lanbahnData.put(lbadr, lbdata);  // update (or create) data    
            // send lanbahnData
            return "XL " + lbadr + " " + lanbahnData.get(lbadr);
        } else {
            // we are using SX
            int sxaddr = lbadr / 10;
            if (!isValidSXAddress(sxaddr)) {
                return "ERROR"; // for SX addresses like "120"    
            }            // depending on nBits() function, only data 0 ... 15 is allowed

            // lanbahn address to SX address mapping: divide by 10, modulo is sxbit
            // for example 987 => SX-98, bit 7 (!! bit from 1 .. 8)
            // must fit into SX channel range, maximum 1278 is allowed !!
            int sxadr = lbadr / 10;
            int sxdata = sxData[sxadr][sxbusControl];  // => current SX data
            int bit = lbadr % 10;
            if ((bit < 1) || (bit > 8)) {
                return "ERROR";
            }

            if (nBits(lbadr) == 1) {
                if (lbdata == 1) {
                    sxdata |= 1 << (bit - 1); // set bit 
                } else if (lbdata == 0) {
                    sxdata &= ~(1 << (bit - 1)); // clear bit 
                }
            } else if (nBits(lbadr) == 2) {
                switch (lbdata) {
                    case 0:
                        sxdata &= ~(1 << (bit - 1)); // clear bit  
                        sxdata &= ~(1 << (bit + 1 - 1)); // clear bit+1
                        break;
                    case 1:
                        sxdata |= 1 << (bit - 1); // set bit  
                        sxdata &= ~(1 << (bit + 1 - 1)); // clear bit+1
                        break;
                    case 2:
                        sxdata &= ~(1 << (bit - 1)); // clear bit
                        sxdata |= 1 << (bit + 1 - 1); // set bit+1  
                        break;
                    case 3:
                        sxdata |= 1 << (bit - 1); // set bit
                        sxdata |= 1 << (bit + 1 - 1); // set bit+1  
                        break;
                    default:
                        sxdata = INVALID_INT;
                    // do not do anything for other values                                     
                }
            }
            if (sxdata != INVALID_INT) {
                sxData[sxadr][sxbusControl] = sxdata;
                sxi.send2SXBusses(sxadr, sxdata);
                String res = "X " + sxadr + " " + sxdata;   // SX Feedback
                res += ";XL " + lbadr + " " + lbdata; // lanbahn feedback
                return res;
            } else {
                System.out.println("ERROR, could not get SX Value");
                return "ERROR";
            }

            /* TODO    check mapping for the following bit also
                    switch (sx.nbit) {
....
                        case 3:
                            dOut = (d >> (sx.bit - 1)) & 0x07;  // three consecutive bits
                            break;
                        case 4:
                            dOut = (d >> (sx.bit - 1)) & 0x0f;  // four consecutive bits
                            break;
 . */
        }
    }

    int nBits(int lbaddr) {
        // TODO implement
        // if a single lanbahnchannel has more than 0/1 values for example
        // for multi-aspect signals
        return 1;
    }

    /** calculate the lanbahn value from state of SX system (only controlbus)
     * at a given sxaddr = lbaddr / 10 and sxbit = lbaddr % 10
     * 
     * @param lbAddress
     * @return lbValue (or INVALID_INT)
     * 
     * (TODO implement for range of sxaddr 128 ..255 */
    
    private int getLanbahnValueFromSXControlBus(int lbaddr) {
        if (DEBUG) {
            System.out.println("getLanbahnValueFromSXdata");
        }
        if (lbaddr > LBMIN_LB) {
            return ERROR; // should not happen
        }
        int sxadr = lbaddr / 10;

        if (!isValidSXAddress(sxadr)) {
            return ERROR;
        }

        int d = sxData[sxadr][sxbusControl];  // => current SX data
        int lbvalue = ERROR;
        int bit = lbaddr % 10;
        if ((bit < 1) || (bit > 8)) {
            return ERROR;
        }

        switch (nBits(lbaddr)) {
            case 1:
                lbvalue = (d >> (bit - 1)) & 0x01;  // sxBit = 1...8
                break;
            case 2:
                lbvalue = (d >> (bit - 1)) & 0x03;  // two consecutive bits
                break;
            case 3:
                lbvalue = (d >> (bit - 1)) & 0x07;  // three consecutive bits
                break;
            case 4:
                lbvalue = (d >> (bit - 1)) & 0x0f;  // four consecutive bits
                break;
            default:
                break;
        }
        if (DEBUG) {
            System.out.println("lbaddr=" + lbaddr + " lbvalue=" + lbvalue);
        }
        return lbvalue;
    }

    private synchronized boolean try_set_sx_accessory(SXAddrAndBits sx, int data) {
        if (!sxi.isConnected()) {
            System.out.println("could not set SX, interface not connected");
            return false;
        }
        if ((sx.bit >= 1) && (sx.bit <= 8) && (sx.nbit <= 4) && (sx.nbit >= 1)) {
            int d = sxData[sx.sxAddr][sxbusControl];
            switch (sx.nbit) {
                case 1:
                    if (data > 1) {
                        System.out.println("could not set SX, data >1 (nbit = 1)");
                        return false;
                    }
                    d = SXUtils.bitOperation(d, sx.bit, (data & 0x01));
                    break;
                case 2:
                    if (data > 3) {
                        System.out.println("could not set SX, data >3 (nbit = 2)");
                        return false;
                    }
                    d = SXUtils.bitOperation(d, sx.bit, (data & 0x01));
                    d = SXUtils.bitOperation(d, sx.bit + 1, (data & 0x02));
                    break;
                case 3:
                    if (data > 7) {
                        System.out.println("could not set SX, data >7 (nbit = 3)");
                        return false;
                    }
                    d = SXUtils.bitOperation(d, sx.bit, (data & 0x01));
                    d = SXUtils.bitOperation(d, sx.bit + 1, (data & 0x02));
                    d = SXUtils.bitOperation(d, sx.bit + 2, (data & 0x04));
                    break;
                case 4:
                    if (data > 15) {
                        System.out.println("could not set SX, data >15 (nbit = 4)");
                        return false;
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
            return true;
        } else {
            if (DEBUG) {
                System.out.println("could not set sx-adr=" + sx.sxAddr + " bit=" + sx.bit);
            }
            return false;
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

    private int getLanbahnDataFromString(String s) {
        // converts String to integer between 0 and 15
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

    /** extract the selectrix address from a string, only valid addresses
     * 0...111,127 and 128..139,255 are allowed, else "INVALID_INT" is returned
     * @param s
     * @return addr (or INVALID_INT)
     */
    int getSXAddrFromString(String s) {
        if (DEBUG) System.out.println("get SXAddr from " + s);
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
    
    /** is the address a valid SX0 or SX1 address ?
     * 
     * @param address 
     * @return true or false
     */
    private boolean isValidSXAddress(int a) {

        if (((a >= SXMIN) && (a <= SXMAX)) || (a == SXPOWER)) {
            if (DEBUG) System.out.println("isValidSXAddress? "+a + " true (SX0");
            return true;  // 0..111 or 127
        }
        if (useSX1forControl) {
            a = a - 128;
            if (((a >= SXMIN) && (a <= SXMAX)) || (a == SXPOWER)) {
                if (DEBUG) System.out.println("isValidSXAddress? "+a + " true (SX1)");
                return true;  // 128..239 or 255
            }
        }
        if (DEBUG) System.out.println("isValidSXAddress? "+a + " false");
        return false;
    }
    
    /** parse String to extract a lanbahn address
     * 
     * @param s
     * @return lbaddr (or INVALID_INT)
     */
    int getLanbahnAddrFromString(String s) {
        //System.out.println("getLanbahnAddrFromString s=" + s);
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

    /**  if channel data changed, send update to clients
     * 
     * @param bus (0 or 1)
     * @param sxaddr (valid sxaddr)
     */
    private void sendSXUpdates(int bus, int sxaddr) {
  
        sxDataCopy[sxaddr][bus] = sxData[sxaddr][bus];
        int chan = sxaddr + (bus * 128);
        String msg = "X " + chan + " " + sxDataCopy[sxaddr][bus];  // SX Feedback Message
        if (DEBUG) {
            System.out.println("sent: " + msg + " (bus=" + bus +")");
        }
        // check for dependent "lanbahn" feedback
        if (bus == 0) {  // only for control bus, BUS=0, i.e. sxadr <= 127
            for (int i = 1; i <= 8; i++) {
                // convert SX data to lanbahn
                int lbaddr = sxaddr * 10 + i;
                int sxvalue = sxDataCopy[sxaddr][0];
                int lbvalue = 0;
                switch (nBits(lbaddr)) {
                    case 1:
                        lbvalue = sxvalue >> (i - 1) & 0x01;   // check if bit 'i' is set
                        break;
                    case 2:
                        lbvalue = sxvalue >> (i - 1) & 0x03;   // 2 bits                   
                        break;
                    case 3:
                        lbvalue = sxvalue >> (i - 1) & 0x07;   // 3 bits                    
                        break;
                    case 4:
                        lbvalue = sxvalue >> (i - 1) & 0x0f;   // 4 bits                    
                        break;
                    default:
                        break;
                }
                msg += ";XL " + lbaddr + " " + lbvalue;  // Lanbahn Message
            }
        }
        if (DEBUG) {
            System.out.println("TL:" + msg);
        }
        sendMessage(msg);  // send all messages, separated with ";"

    }

    /**
     * check for changed sxData and send update in case of change
     */
    private void checkForChangedSXDataAndSendUpdates() {

        // power channel
        if (sxData[127][0] != sxDataCopy[127][0]) {
            sendSXUpdates(0, 127);
        }
        // other channels
        for (int bus = 0; bus < 2; bus++) {
            for (int ch = 0; ch < 112; ch++) {
                if (sxData[ch][bus] != sxDataCopy[ch][bus]) {
                    // channel data changed, send update to mobile device
                    sendSXUpdates(bus, ch);
                }
            }
        }
    }

    /**
     * check for changed (exclusiv) lanbahn data and send update in case of
     * change
     *
     */
    private void checkForLanbahnChangesAndSendUpdates() {
        StringBuilder msg = new StringBuilder();
        boolean first = true;
        for (Map.Entry<Integer, Integer> e : lanbahnData.entrySet()) {
            Integer key = e.getKey();
            Integer value = e.getValue();
            if (lanbahnDataCopy.containsKey(key)) {
                if (lanbahnDataCopy.get(key) != lanbahnData.get(key)) {
                    // value has changed
                    lanbahnDataCopy.put(key, value);
                    if (!first) {
                        msg.append(";");
                    }
                    msg.append("XL " + key + " " + value);
                    first = false;
                    if (msg.length() > 60) {
                        sendMessage(msg.toString());
                        msg.setLength(0);  // =delete content
                        first = true;
                    }
                }
            } else {
                lanbahnDataCopy.put(key, value);
            }
        }
        if (msg.length() > 0) {
            sendMessage(msg.toString());
        }
    }
}
