/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.MainUI.INVALID_INT;
import static de.blankedv.sx3pc.MainUI.SXMAX;
import static de.blankedv.sx3pc.MainUI.allSignalMappings;

/**
 *
 * @author mblank
 */
public class SignalMapping {
    int lbAddr;
    int sxAddr;
    int sxBit;
    int nBit;
    
    SignalMapping() {
        lbAddr = INVALID_INT;
        sxAddr = INVALID_INT;
        sxBit = INVALID_INT;
        nBit = INVALID_INT;
    }
    
    SignalMapping(int lba, int nBits) {
        this.lbAddr = lba;
        this.sxAddr = lba/10;
        if ((sxAddr < 1) || (sxAddr > SXMAX)) {
            this.lbAddr = INVALID_INT;   // not possible to create a mapping
        } else {
            this.sxBit = lbAddr % 10;
            this.nBit = nBits;
        }
    }
    
    void setSXBits(int lba, int value) {
        // TODO
    }
    
    static boolean exists(int lba) {
       for (SignalMapping sm : allSignalMappings) {
           if (sm.lbAddr == lba) return true;
       }
       return false;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder("");
        sb.append("lbAddr=");
        sb.append(lbAddr);
        sb.append(" sxAddr=");
        sb.append(sxAddr);
        sb.append(" sxBit=");
        sb.append(sxBit);
        sb.append(" nBit=");
        sb.append(nBit);
        return sb.toString();
    }
}
