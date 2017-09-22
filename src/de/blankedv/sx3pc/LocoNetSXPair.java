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
public class LocoNetSXPair {

        public int lnAddr;   // 1. LN address, using 8 LN addresses lnAddr .. lnAddr+7
        public int sxAddr;   // sx Address
        
        LocoNetSXPair() {
            lnAddr = INVALID_INT;
            sxAddr = INVALID_INT;

        }
        
        LocoNetSXPair(int l, int s) {
            lnAddr = l;
            sxAddr = s;

        }
        
        
        /**
         * calculate lanbahn value from the SX data byte
         * use only relevant bits sxBit ... sxBit+(nBit-1)
         * @param d
         * @return 
         */
        /* public int getLBValueFromSXByte(int d) {         
            int v = 0;
            for (int i = sxBit; i < (sxBit+nBit); i++) {
                if (SXUtils.isSet(d,i) != 0) {
                    v = v + (1 << (i - sxBit));
                }               
            }
            //if (sxAddr == 70) {
            //System.out.println("lbaddr="+lbAddr+ " sxaddr="+sxAddr+ " sxBit="+sxBit+" nBit="+nBit+" v="+v);
            //}
            return v;
        } */
        
        public boolean isValid() {
            if ((lnAddr != INVALID_INT) &&
                    (sxAddr != INVALID_INT) ) {
                return true;
            } else {
                return false;
            }
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("lnAddr=");
            sb.append(lnAddr);
            sb.append(" sxAddr=");
            sb.append(sxAddr);
            return sb.toString();
        }

}
