/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.MainUI.DEBUG;
import static de.blankedv.sx3pc.MainUI.SXMAX2;
import static de.blankedv.sx3pc.MainUI.sxData;
import static de.blankedv.sx3pc.MainUI.sxbusControl;
import static de.blankedv.sx3pc.MainUI.useSX1forControl;

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
    
 
    public boolean isConnected() {
        return connected;
    }
    
    public String doUpdate() {
        ;  // implemented in SXFCCInterface, where a full update can be
        // requested regularly
        return "";
    }

    public synchronized void send2SXBussesBit(int adr, int bit, int data) {
        // accepts adresses >127 and then sends data to SX1 (instead of SX0)
        //  sxData[adr];
        // twoBusses = false;
        // locos always control on SX0, "schalten/melden" on SX0 or SX1
        // static int sxbusControl = 0;
        int bus = 0;
        if (adr > SXMAX2) {
            if (useSX1forControl == true) {
                bus = 1;
                adr = adr - 128;
            } else {
                System.out.println("ERROR, trying to send to channel=" + adr + ", but only one SX Bus enabled.");
            }
        }
        int d = sxData[adr][bus];
        Byte[] b = {(byte) (adr + 128), 0};  // bit 7 muss gesetzt sein zum Schreiben
        if (data == 1) {  // set bit
            d |= (1 << (bit - 1));  // sx bit von 1 bis 8
        } else {
            // reset bit
            d = d & ~(1 << (bit - 1));  // sx bit von 1 bis 8
        }
        b[1] = (byte) (d);
        send(b, bus);

    }

    /** 
     * send a data byte to a specific bus number
     * busses are activated)
     * @param data (Byte[], first byte address, second byte data
     * @param busnumber (either 0=> SX0 or 1=>SX1)
     */
    abstract public void send(Byte[] data, int busnumber);

    /** 
     * set or reset a bit on the control channel (SX0 when only 1 bus, SX1 when
     * busses are activated)
     * @param adr
     * @param bit
     * @param data 
     */
    public synchronized void sendAccessoryBit(int adr, int bit, int data) {
        int d = sxData[adr][sxbusControl];
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
        send(b, sxbusControl);
    }

    /**
     * sends and selectrix control command to the SX interface
     * if adr > 128 => output sent to SX1
     *    adr <=128 (=SXMAX2) => output sent to SX0
     * 
     * @param adr
     * @param data 
     */
    public synchronized void send2SXBusses(int adr, int data) {
        // accepts adresses >127 and then sends data to SX1 (instead of SX0)
        // locos always control on SX0, "schalten/melden" on SX0 or SX1
        if (adr > SXMAX2) {
            if (useSX1forControl == true) {
                adr = adr - 128;  // to make channel number clear.
                Byte[] b = {(byte) (adr + 128), (byte) data};  // bit 7 muss gesetzt sein zum Schreiben
                send(b, 1);
            } else {
                System.out.println("ERROR, trying to send to channel=" + adr + ", but only one SX Bus enabled.");
            }
        } else {
            Byte[] b = {(byte) (adr + 128), (byte) data};  // bit 7 muss gesetzt sein zum Schreiben
            send(b, 0);
        }
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
    public synchronized void sendLoco(int lok_adr, int speed, boolean licht, boolean forward, boolean horn) {
        // constructs SX loco data from speed and bit input.
        int data = 0;

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
        send(b, 0);
    }

    abstract public void registerFeedback(int adr);

    abstract public void switchPowerOn();

    abstract public void switchPowerOff();

    abstract public void readPower();

    public void resetAll() {

        Byte[] b = {(byte) 0xFF, (byte) 0x00};
        for (int i = 0; i < 112; i++) {
            b[0] = (byte) ((i & 0x7F) | 0x80);  // write command
            send(b, 0);
            try {
                Thread.sleep(50);
            } catch (Exception e) {
            };
        };

        if (useSX1forControl) {
            for (int i = 0; i < 112; i++) {
                b[0] = (byte) ((i & 0x7F) | 0x80);  // write command
                send(b, 1);
                try {
                    Thread.sleep(50);
                } catch (Exception e) {
                };
            };
        }
    }

    public void unregisterFeedback() {
    }
    ;

    public void unregisterFeedback(int adr) {
    }
    ;

    public synchronized void sendAccessory(int adr, int data) {
        //  sxData[adr];
        Byte[] b = {(byte) (adr + 128), (byte) data};  // bit 7 muss gesetzt sein zum Schreiben
        send(b, sxbusControl);
    }

}
