/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.MainUI.INVALID_INT;

/**
 * Pair of SX-Address (0 ..111, 127) and a bit (1...8)
 * @author mblank
 */
public class SxAbit {
    public int addr;
    public int bit;
    
    SxAbit (int a, int b) {
        addr = a;
        bit = b;
    }
    
    public String toString() {
        StringBuilder s = new StringBuilder();
        if ((addr != INVALID_INT) && (bit != INVALID_INT)) {
             s.append(addr);
        } else {
            return ("INVALID");
        }
        s.append(".");
        s.append(bit);
        return s.toString();       
    }
    
}
