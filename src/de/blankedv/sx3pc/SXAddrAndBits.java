/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.MainUI.INVALID_INT;
/**
 *
 * @author mblank
 */
class SXAddrAndBits {
    int sxAddr;
    int bit;
    int nbit;
    
    SXAddrAndBits(int a, int b) {
        sxAddr = a;
        bit = b;
        nbit = 1;
        
    }
    
    SXAddrAndBits(int a, int b, int n) {
        sxAddr = a;
        bit = b;
        nbit = n;
        
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        if (sxAddr == INVALID_INT) {
            return "invalid";
        } else {
            sb.append(" sxAddr=");
            sb.append(sxAddr);
            sb.append(" bits=");
            for (int i=nbit; i>=1; i--) {
               sb.append((bit+i-1));           
            }
            return sb.toString();
        }
    }
}
