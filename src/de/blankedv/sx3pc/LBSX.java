/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.InterfaceUI.INVALID_INT;

/**
 * is used for mapping of lanbahn addresses to SX addresses
 * 
 * @author mblank
 */
public class LBSX {

        public int lbAddr;
        public int sxAddr;
        public int sxBit;
        public int sxBit2;
        
        LBSX() {
            lbAddr = INVALID_INT;
            sxAddr = INVALID_INT;
            sxBit = 1;
            sxBit2 = INVALID_INT;
        }
        
        LBSX(int l, int s, int b, int b2) {
            lbAddr = l;
            sxAddr = s;
            sxBit = b;
            sxBit2 = b2;
        }
        
        LBSX(int l, int s, int b) {
            lbAddr = l;
            sxAddr = s;
            sxBit = b;
            sxBit2 = INVALID_INT;
        }
        
        public boolean isValid() {
            if ((lbAddr != INVALID_INT) &&
                    (sxAddr != INVALID_INT) &&
                    (sxBit >=1) &&
                     (sxBit <=8)) {
                return true;
            } else {
                return false;
            }
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("lbAddr=");
            sb.append(lbAddr);
            sb.append(" sxAddr=");
            sb.append(sxAddr);
            sb.append(" b1=");
            sb.append(sxBit);
            if (sxBit2 != INVALID_INT) {
                sb.append(" b2=");
                sb.append(sxBit2);
            }
            return sb.toString();
        }

}
