/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.MainUI.DEBUG;
import static de.blankedv.sx3pc.MainUI.INVALID_INT;
import static de.blankedv.sx3pc.MainUI.SXMAX2;
import static de.blankedv.sx3pc.MainUI.connectionOK;
import static de.blankedv.sx3pc.MainUI.lanbahnData;
import static de.blankedv.sx3pc.MainUI.sxData;
import static de.blankedv.sx3pc.MainUI.sxbusControl;
import static de.blankedv.sx3pc.MainUI.vtest;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * for opensx USB interface
 *
 * see https://opensx.net
 *
 *
 * @author mblank
 */
public class SXOpenSXInterface extends GenericSXInterface implements SerialPortEventListener {

    private String portName;

    private int baudrate = 115200;
    private int dataBits = SerialPort.DATABITS_8;
    private int stopBits = SerialPort.STOPBITS_1;
    private int parity = SerialPort.PARITY_NONE;
    CommPortIdentifier serialPortId;
    Enumeration enumComm;
    SerialPort serialPort;
    private BufferedReader input;
    private OutputStream output;
    Boolean serialPortGeoeffnet = false;
    private int lastAdrSent = -1;
    private SXInterface.PollingActivity pa;

    private List<Integer> pListCopy;

    Boolean regFeedback = false;
    int regFeedbackAdr = 0;
    
    private long lastXrequest = System.currentTimeMillis();

    SXOpenSXInterface(String port) {
        this.portName = port;
    }
    
    public void setPort(String port) {
        portName = port;
    }

    @Override
    public boolean open() {
        Boolean foundPort = false;
        if (serialPortGeoeffnet != false) {
            System.out.println("Serialport bereits geöffnet");
            return false;
        }
        System.out.println("Öffne Serialport " + portName);
        enumComm = CommPortIdentifier.getPortIdentifiers();
        while (enumComm.hasMoreElements()) {
            serialPortId = (CommPortIdentifier) enumComm.nextElement();
            if (portName.contentEquals(serialPortId.getName())) {
                foundPort = true;
                break;
            }
        }
        if (foundPort != true) {
            System.out.println("Serialport nicht gefunden: " + portName);
            return false;
        }
        try {
            serialPort = (SerialPort) serialPortId.open("Öffnen und Senden", 500);
        } catch (PortInUseException e) {
            System.out.println("Port belegt");
        }

        try {
            input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
            output = serialPort.getOutputStream();
        } catch (IOException ex) {
            System.out.println("Konnte Streams nicht öffnen");
        }

        try {
            serialPort.addEventListener(this);
        } catch (TooManyListenersException ex) {
            System.out.println("Konnte EventListener nicht hinzufügen");
        }
        serialPort.notifyOnDataAvailable(true);

        try {
            serialPort.setSerialPortParams(baudrate, dataBits, stopBits, parity);
        } catch (UnsupportedCommOperationException e) {
            System.out.println("Konnte Schnittstellen-Paramter nicht setzen");
        }

        serialPortGeoeffnet = true;
        connected = true;

        return true;
    }

    @Override
    public synchronized void serialEvent(SerialPortEvent oEvent) {
        if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                String inputLine = null;
                if (input.ready()) {
                    inputLine = input.readLine();
                    interpretMessage(inputLine);
                }

            } catch (Exception e) {
                System.err.println(e.toString());
            }
        }
        // Ignore all the other eventTypes, but you should consider the other ones.
    }

    @Override
    public void close() {
        if (serialPortGeoeffnet == true) {
            System.out.println("Schließe Serialport");
            serialPort.close();
            serialPortGeoeffnet = false;
        } else {
            System.out.println("Serialport bereits geschlossen");
        }
        connected = false;
    }

    @Override
    public String doUpdate() {
        return ""; // TODO
        /*
        if ((System.currentTimeMillis() -lastXrequest) < 5000) return;
        lastXrequest = System.currentTimeMillis();
        sendMsg("X");  // request to send all SX channel */
    }

    private void shortSleep() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            Logger.getLogger(SXOpenSXInterface.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void registerFeedback(int adr
    ) {
        // not necessary, because it is polled every second
    }

    @Override
    public void switchPowerOn() {

        sendMsg("S 127 1");
        sxData[127][0] = 1;
    }

    @Override
    public void switchPowerOff() {

        sendMsg("S 127 0");
        sxData[127][0] = 0;
    }

    @Override
    public void unregisterFeedback(int adr
    ) {
        // not necessary, because it is polled every second
    }

    @Override
    public void resetAll() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unregisterFeedback() {
        // not necessary, because it is polled every second
    }

    @Override
    public void readPower() {
        // not necessary, because it is polled every second
        connectionOK = true;
    }

    private void sendMsg(String msg) {
        if (serialPortGeoeffnet != true) return;
        
        try {
            output.write(msg.getBytes(Charset.forName("UTF-8")));
            output.write('\n');
            output.flush();
            // done via polling in LanbahnUI // doLanbahnUpdate((byte)(data[0] & 0x7f), data[1]);
        } catch (IOException e) {
            System.out.println("Fehler beim Senden");
        }
    }

    @Override
    public void send(Byte[] data, int busnumber) {
        if (busnumber != 0) return;  //only using SX0 bus
        
        StringBuilder msg = new StringBuilder("S ");
        int ad = 0x7f & ((byte)data[0] ) ;
        sxData[ad][0] = data[1];
        msg.append(""+ ad);   // convert binary channel to decimal ascii number
        msg.append(" " + data[1]);  // convert binary data to decimal ascii number
        sendMsg(msg.toString());
    }

    /** interpret received message and update sxData array
     * 
     * @param s 
     */
    private void interpretMessage(String s) {

        int addr, data;

        if (s.length() == 0) {
            return;
        }
        String[] cmd = s.split(" ");
        if ((cmd == null) || (cmd.length <= 2) || (cmd[0].toLowerCase().charAt(0) != 'f')) {
            return; // we are only interested in feedback messages
        }
        try {
            if (cmd.length >= 3) {
                addr = Integer.parseInt(cmd[1]);
                data = Integer.parseInt(cmd[2]);

                if (addr >= 0 && addr < SXMAX2) {
                    // data for SX0 bus
                    sxData[addr][0] = data;
                    if ((regFeedback) && (sxbusControl == 0)
                            && (addr == regFeedbackAdr)) {
                        vtest.sensorFeedback(data);
                    }
                    if (DEBUG) {
                        //System.out.print("SX0[" + addr + "]=" + data + " ");
                    }
                }
            }
        } catch (NumberFormatException e) {
           System.out.println("Fehler beim Empfangen: "+s);
        }
    }
}
