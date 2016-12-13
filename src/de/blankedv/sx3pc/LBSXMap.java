/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.InterfaceUI.INVALID_INT;
import static de.blankedv.sx3pc.InterfaceUI.lbsx;
import java.util.ArrayList;
/**
 * utility function for the mapping of lanbahn addresses to SX addresses (and 
 * bits) and vice versa
 * 
 * @author mblank
 */
public class LBSXMap {
   
    // TODO second sxbit
    
    public static void init() {
        // init LBSXMap      
       // lbsx.add(new LBSX(722, 72, 2));
       // lbsx.add(new LBSX(721, 74, 1));
    }
    
    public static int getLanbahn(int sxaddr, int sxbit) {
        for (LBSX ls:lbsx) { 
            if ((ls.sxAddr == sxaddr) && (ls.sxBit == sxbit))  {
                return ls.lbAddr;
            }
        }
        return INVALID_INT; // => no mapping
    }
    
    public static SXAddrAndBit getSX(int lbAddr) {
        for (LBSX ls:lbsx) {
            if (ls.lbAddr == lbAddr) {
                return new SXAddrAndBit(ls.sxAddr, ls.sxBit, ls.sxBit2);
            }
        }
        return new SXAddrAndBit(INVALID_INT, 0);   // => no mapping
    }
    
    
}
