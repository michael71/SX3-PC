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
public class LanbahnSXPair {

        public int lbAddr;   // lanbahn address
        public int sxAddr;   // sx Address
        public int sxBit;    // (first) sx bit (1..8) (== lowest bit value
                          // example sxBit = 5, nBit=2
                          // bit5=1 ==> value = 1
                          // bit6=1 ==> value = 2
                          // bit5 and bit6 =set => Value = 3
        
        public int nBit;    // number of bits used 1 ...4
        
        LanbahnSXPair() {
            lbAddr = INVALID_INT;
            sxAddr = INVALID_INT;
            sxBit = 1;
            nBit = 1;
        }
        
        LanbahnSXPair(int l, int s, int b, int n) {
            lbAddr = l;
            sxAddr = s;
            sxBit = b;
            nBit = n;
        }
        
        LanbahnSXPair(int l, int s, int b) {
            lbAddr = l;
            sxAddr = s;
            sxBit = b;
            nBit = 1;
        }
        
        /**
         * calculate lanbahn value from the SX data byte
         * use only relevant bits sxBit ... sxBit+(nBit-1)
         * @param d
         * @return 
         */
        public int getLBValueFromSXByte(int d) {         
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
            for (int i= nBit; i >=1; i--) {
                sb.append(" bit=");
                sb.append((sxBit+(i-1)));
            }
            return sb.toString();
        }

}
