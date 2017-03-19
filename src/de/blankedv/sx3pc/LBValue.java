/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.InterfaceUI.INVALID_INT;
/**
 *
 * @author mblank
 */
class LBValue {
    int lbAddr;
    int lbValue;
    
    LBValue() {
        lbAddr = INVALID_INT;
        lbValue = 0;
    }
    LBValue(int a, int v) {
        lbAddr = a;
        lbValue = v;     
    }
   
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        if (lbAddr == INVALID_INT) {
            return "invalid";
        } else {
            sb.append("lbAddr=");
            sb.append(lbAddr);
            sb.append(" v=");
            sb.append(lbValue);
            
            return sb.toString();
        }
    }
}
