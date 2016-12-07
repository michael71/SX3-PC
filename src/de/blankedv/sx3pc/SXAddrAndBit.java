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
class SXAddrAndBit {
    int sxAddr;
    int bit;
    int bit2;
    
    SXAddrAndBit(int a, int b) {
        sxAddr = a;
        bit = b;
        bit2 = INVALID_INT;
        
    }
    
    SXAddrAndBit(int a, int b, int b2) {
        sxAddr = a;
        bit = b;
        bit2 = b2;
        
    }
}
