/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.blankedv.sx3pc;
import static de.blankedv.sx3pc.InterfaceUI.sxData;   // SX Data
import static de.blankedv.sx3pc.InterfaceUI.sxi;   // the SX interface.
/**
 *
 * @author mblank
 */
public class Loco {
    private boolean forward = true;
    private int speed = 0;
    private boolean licht = false;
    private boolean horn = false;
    private int lok_adr = 1;
 
    public Loco() {
    }

    public boolean isForward() {
        return forward;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public boolean isHorn() {
        return horn;
    }

    public void setHorn(boolean horn) {
        this.horn = horn;
    }

    public boolean isLicht() {
        return licht;
    }

    public void setLicht(boolean licht) {
        this.licht = licht;
    }

    public int getLok_adr() {
        return lok_adr;
    }

    public void setLok_adr(int lok_adr) {
        this.lok_adr = lok_adr;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        if (speed <0 ) speed = 0;
        if (speed >31) speed = 31;
        this.speed = speed;
    }

//    public void updateToSX() {
//        int data = 0;
//        // Berechnen des Steuerwort
//        data = speed;  // die unteren 5 bits (0..4)
//        if (horn) {
//            data += 128; // bit7
//        }
//        if (licht) {
//            data += 64; // bit6
//        }
//        if (forward == false) {
//            data += 32; //bit5
//        }
//        //System.out.println("update loco " + Integer.toHexString(data));
//        Byte[] b = {(byte) (lok_adr + 128), 0};  // bit 7 muss gesetzt sein zum Schreiben
//        b[1] = (byte) data;
//        sxi.send(b,0);
//    }

//    public void updateFromSX() {
//        // initial werte lesen aus sxData
//        int ld = sxData[lok_adr][0];
//        speed = (ld & 0x1F);
//        horn = ((ld & 0x80) == 0x80);
//        licht = ((ld & 0x40) == 0x40);
//        forward = !((ld & 0x20) == 0x20);
//    }
}
