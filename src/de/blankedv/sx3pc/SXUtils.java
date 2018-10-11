/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.MainUI.INVALID_INT;
import static de.blankedv.sx3pc.MainUI.SXMAX;
import static de.blankedv.sx3pc.MainUI.SXMIN;
import static de.blankedv.sx3pc.MainUI.SXPOWER;
import static de.blankedv.sx3pc.MainUI.panelElements;
import static de.blankedv.sx3pc.MainUI.sxData;
import static de.blankedv.sx3pc.MainUI.sxi;

import de.blankedv.timetable.PanelElement;

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

        if ((bit >= 1) && (bit <= 8)) {
            return true;  // 1..8
        }

        //if (DEBUG) System.out.println("isValidSXAddress? "+a + " false");
        return false;
    }

    public static SXAddrAndBits lbAddr2SX(int lbAddr) {
        if (lbAddr == INVALID_INT) {
            return null;
        }
        int a = lbAddr / 10;
        int b = lbAddr % 10;
        if (isValidSXAddress(a) && isValidSXBit(b)) {
            return new SXAddrAndBits(a, b, 1);  // TODO generalize for multibit addresses
        } else {
            return null;
        }
    }

    public static void setPanelElementStateFromSX(int sxAddr, int data) {
        for (int bit = 1; bit <= 8; bit++) {
            // check if we have a lanbahn entry
            int lbAddr = sxAddr * 10 + bit;
            for (PanelElement pe : panelElements) {
                if (pe.getAdr() == lbAddr) {
                    int dataBit = isSet(data, bit);
                    int state = pe.getState();
                    if (dataBit == 0) {
                        state = state & (~1);  // clear last bit
                    } else {
                        state = state | 1;     // set last bit
                    }
                    pe.setState(state);
                }
                if (pe.getSecondaryAdr() == lbAddr) {
                    int dataBit = isSet(data, bit);
                    int state = pe.getState();
                    if (dataBit == 0) {
                        state = state & (~2);  // clear bit1
                    } else {
                        state = state | 2;     // set bit1
                    }
                    pe.setState(state);
                }
            }
        }
    }
}
