/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.InterfaceUI.DEBUG;
import static de.blankedv.sx3pc.InterfaceUI.sxData;
import static de.blankedv.sx3pc.InterfaceUI.useSX1forControl;
import static de.blankedv.sx3pc.SXInterface.toUnsignedInt;
import java.io.IOException;

/**
 *
 * @author mblank
 */
public class SXSimulationInterface extends GenericSXInterface {

    Boolean serialPortGeoeffnet = false;

    SXSimulationInterface() {

    }

    @Override
    public boolean open() {
        serialPortGeoeffnet = true;
        return true;
    }

    @Override
    public void close() {

    }
    
    
    @Override
    public void registerFeedback(int adr) {
        // not necessary, because it is polled every second
        // TODO ?? check
    }

    @Override
    public void switchPowerOn() {

        sxData[127][0] = 0x80;

    }

    @Override
    public void switchPowerOff() {

        sxData[127][0] = 0x00;

    }

    @Override
    public void unregisterFeedback(int adr) {
        // not necessary, because it is polled every second
        // TODO ?? check
    }

    
   

    @Override
    public void unregisterFeedback() {
        // not necessary, because it is polled every second
    }

    @Override
    public void readPower() {
        // not necessary, because it is polled every second
    }

    @Override
    public synchronized void send(Byte[] data, int busnumber) {
        // only write commands in simulation
        // convert "signed" byte to unsigned int.
        // (byte is ALLWAYS signed in Java)
        sxData[data[0] & 0x7F][busnumber] = toUnsignedInt(data[1]);
    }

    

    
}
