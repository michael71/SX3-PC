/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.MainUI.INVALID_INT;
import static de.blankedv.sx3pc.MainUI.allLanbahnSXPairs;
/**
 *
 * @author mblank
 */
class LanbahnValue {
    int lbAddr;
    int lbValue;
    
    LanbahnValue() {
        lbAddr = INVALID_INT;
        lbValue = 0;
    }
    LanbahnValue(int a, int v) {
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
