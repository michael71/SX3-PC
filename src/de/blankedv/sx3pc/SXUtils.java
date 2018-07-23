/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.MainUI.SXMAX;
import static de.blankedv.sx3pc.MainUI.SXMIN;
import static de.blankedv.sx3pc.MainUI.SXPOWER;
import static de.blankedv.sx3pc.MainUI.sxData;
import static de.blankedv.sx3pc.MainUI.sxi;

/**
 *
 * @author mblank
 */
public class SXUtils {

    /**
     * returns 1, when bit is set in data d returns 0, when bit is not set in
     * data d
     *
     * @param d
     * @param bit
     * @return
     */
    static public int isSet(int d, int bit) {
        return (d >> (bit - 1)) & 1;
    }

    static private int setBit(int d, int bit) {
        return d | (1 << (bit - 1));  // selectrix bit !!! 1 ..8
    }

    static private int clearBit(int d, int bit) {
        return d & ~(1 << (bit - 1));  // selectrix bit !!! 1 ..8
    }
    
    synchronized static public void setBitSxData(int addr, int bit) {
        sxData[addr] = setBit(sxData[addr], bit);
        sxi.send2SX(addr, sxData[addr]);
    }
    
    synchronized static public void clearBitSxData(int addr, int bit) {
        sxData[addr] = clearBit(sxData[addr], bit);
        sxi.send2SX(addr, sxData[addr]);
    }
    
    synchronized static public void setSxData(int addr, int data) {
        sxData[addr] = data;
        sxi.send2SX(addr, data);
    }
    
    /**
     * set or clear a bit depending on "value" variable
     *
     * @param d data value
     * @param bit bit (selectrix bit, 1...8 )
     * @param value (0 or 1)
     * @return new data value
     */
    static public int bitOperation(int d, int bit, int value) {
        if (value == 0) {
            return clearBit(d, bit);
        } else {
            return setBit(d, bit);
        }
    }

    /**
     * is bit (1...8) different in d1 and d2
     *
     */
    static public boolean isSXBitChanged(int b, int d1, int d2) {
        int d1_bit = (d1 >> (b - 1)) & 1;
        int d2_bit = (d2 >> (b - 1)) & 1;
        if (d1_bit == d2_bit) {
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * is the address a valid SX0 or SX1 address
     *
     * @param address
     * @return true or false
     */
    public static boolean isValidSXAddress(int a) {
        if (((a >= SXMIN) && (a <= SXMAX)) || (a == SXPOWER)) {
            //if (DEBUG) System.out.println("isValidSXAddress? "+a + " true (SX0");
            return true;  // 0..111 or 127
        }

        //if (DEBUG) System.out.println("isValidSXAddress? "+a + " false");
        return false;
    }
    
    /**
     * is the bit a valid SX bit? (1...8) 
     *
     * @param bit
     * @return true or false
     */
    public static boolean isValidSXBit(int bit) {

        if ((bit >= 1) && (bit <=8)) {
            return true;  // 1..8
        }

        //if (DEBUG) System.out.println("isValidSXAddress? "+a + " false");
        return false;
    }

}
