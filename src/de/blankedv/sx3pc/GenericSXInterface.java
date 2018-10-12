/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.MainUI.DEBUG;
import static de.blankedv.sx3pc.MainUI.sxData;


/**
 * abstract class for generic selectrix interfaces
 * 
 * @author mblank
 */
abstract public class GenericSXInterface {

    protected boolean connected = false;
    
    abstract public boolean open();
    
    abstract public void setPort(String port);

    abstract public void close();
    
    abstract public String getPortName();
   
 
    public boolean isConnected() {
        return connected;
    }
    
    public int connState() {
        if (connected) {
            return MainUI.STATUS_CONNECTED;
        } else {
            return MainUI.STATUS_NOT_CONNECTED;
        }
    }
    
    public String doUpdate() {
        ;  // implemented in SXFCCInterface, where a full update can be
        // requested regularly
        return "";
    }

    /** TODO unused? public synchronized void send2SXBit(int adr, int bit, int data) {

        if (adr >= SXMAX) return;
                
        int d = sxData[adr];
        Byte[] b = {(byte) (adr + 128), 0};  // bit 7 muss gesetzt sein zum Schreiben
        if (data == 1) {  // set bit
            d |= (1 << (bit - 1));  // sx bit von 1 bis 8
        } else {
            // reset bit
            d = d & ~(1 << (bit - 1));  // sx bit von 1 bis 8
        }
        b[1] = (byte) (d);
        send(b);

    } */

    /** 
     * send a data byte to a specific bus number
     * busses are activated)
     * @param data (Byte[], first byte address, second byte data
     */
    abstract public boolean send(Byte[] data);

    /** 
     * set or reset a bit on the control channel (SX0 when only 1 bus, SX1 when
     * busses are activated)
     * @param adr
     * @param bit
     * @param data 
     */
    public synchronized boolean sendAccessoryBit(int adr, int bit, int data) {
        int d = sxData[adr];
        Byte[] b = {(byte) (adr + 128), 0};  // bit 7 muss gesetzt sein zum Schreiben
        if (data == 1) {  // set bit
            d |= (1 << (bit - 1));  // sx bit von 1 bis 8
        } else {
            // reset bit
            d = d & ~(1 << (bit - 1));  // sx bit von 1 bis 8
        }
        b[1] = (byte) (d);
        // ???? sxData[adr][sxbusControl] = d;
        if (DEBUG) System.out.println("SendAc: a="+adr+" d="+b[1]);
        return send(b);
    }

    /**
     * sends and selectrix control command to the SX interface
     * if adr > 128 => output sent to SX1
     *    adr <=128 (=SXMAX2) => output sent to SX0
     * 
     * @param adr
     * @param data 
     */
    public synchronized boolean send2SX(int adr, int data) {
        // accepts adresses >127 and then sends data to SX1 (instead of SX0)
        // locos always control on SX0, "schalten/melden" on SX0 or SX1

            Byte[] b = {(byte) (adr + 128), (byte) data};  // bit 7 muss gesetzt sein zum Schreiben
            return send(b);

    }

    public synchronized boolean sendChannel2SX(int adr) {
        if (!SXUtils.isValidSXAddress(adr)) {
            System.out.println("ERROR in sendChannel2SX, adr="+adr+ " is invalid");
            return false;
        }
        return send2SX(adr, sxData[adr]);
    }
    /**
     * sends a loco control command (always SX0 !) to the SX interface
     * 
     * @param lok_adr
     * @param speed
     * @param licht
     * @param forward
     * @param horn 
     */
    public synchronized boolean sendLoco(int lok_adr, int speed, boolean licht, boolean forward, boolean horn) {
        // constructs SX loco data from speed and bit input.
        int data;

        if (speed > 31) {
            speed = 31;
        }
        if (speed < 0) {
            speed = 0;
        }
        if (DEBUG) {
            //System.out.println("adr:" + lok_adr + " s:" + speed + " l:" + licht + " forw:" + forward + " h:" + horn);
        }
        data = speed;  // die unteren 5 bits (0..4)
        if (horn) {
            data += 128; // bit7
        }
        if (licht) {
            data += 64; // bit6
        }
        if (forward == false) {
            data += 32; //bit5
        }
        if (DEBUG) {
            //System.out.println("update loco " + Integer.toHexString(data));
        }
        Byte[] b = {(byte) (lok_adr + 128), 0};  // bit 7 muss gesetzt sein zum Schreiben
        b[1] = (byte) data;
        return send(b);
    }

    abstract public void registerFeedback(int adr);

    abstract public void switchPowerOn();

    abstract public void switchPowerOff();

    abstract public void readPower();

    public void resetAll() {

        Byte[] b = {(byte) 0xFF, (byte) 0x00};
        for (int i = 0; i < 112; i++) {
            b[0] = (byte) ((i & 0x7F) | 0x80);  // write command
            send(b);
  /*          try {
                Thread.sleep(50);
            } catch (Exception e) {
            }; */
        };

    }

    public void unregisterFeedback() {
    }
    ;

    public void unregisterFeedback(int adr) {
    }
    ;

    public synchronized boolean sendAccessory(int adr, int data) {
        //  sxData[adr];
        Byte[] b = {(byte) (adr + 128), (byte) data};  // bit 7 muss gesetzt sein zum Schreiben
        return send(b);
    }

}
