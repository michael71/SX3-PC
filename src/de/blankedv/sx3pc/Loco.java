/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.blankedv.sx3pc;

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

}
