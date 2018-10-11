/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.timetable;


import de.blankedv.sx3pc.LbData;
import static de.blankedv.sx3pc.MainUI.panelElements;
import static de.blankedv.timetable.Vars.*;
import java.util.HashMap;

/**
 *
 * @author mblank
 */
public class LbUtils {



    static public void mySleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            System.out.println("ERROR could not sleep");
        }
    }
    
    /** put addr new value to addr lanbahn address, and keep the type unchanged
 or (if the address does not exist so far, create it new with type ACCESSORY
     * 
     * @param addr
     * @param value 
     */
  /* static public void updateLanbahnData(int addr, int value) {
         LbData lb = lanbahnData.get(addr);
         if (lb == null) {
             // should not happen
             if (DEBUG) System.out.println("ERROR: unknown addr="+addr);
            lb = new LbData(value, 1, "T");
         }
         lanbahnData.put(addr, new LbData(value, lb.getNBit(), lb.getTypeString()));
         for (PanelElement pe : panelElements) {
             if (pe.adr == addr) {
                 pe.state = value;
             }
         }
         if (lanbahnData.get(addr).getData() != value) {
             System.out.println("ERROR setting addr="+addr+" to val="+value+" not successful");
         }
    }
    
    static public void createLanbahnData(int addr, int nbit, String type) {
        lanbahnData.put(addr, new LbData(0, nbit, type));
    } */

    
}
