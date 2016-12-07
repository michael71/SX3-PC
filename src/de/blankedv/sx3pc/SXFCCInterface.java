/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.InterfaceUI.DEBUG;
import static de.blankedv.sx3pc.InterfaceUI.connectionOK;
import static de.blankedv.sx3pc.InterfaceUI.sxData;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mblank
 */
public class SXFCCInterface extends GenericSXInterface {

    private String portName;

    private int baudrate = 230400;
    private int dataBits = SerialPort.DATABITS_8;
    private int stopBits = SerialPort.STOPBITS_1;
    private int parity = SerialPort.PARITY_NONE;
    CommPortIdentifier serialPortId;
    Enumeration enumComm;
    SerialPort serialPort;
    OutputStream outputStream;
    InputStream inputStream;
    Boolean serialPortGeoeffnet = false;
    private int lastAdrSent = -1;
    private SXInterface.PollingActivity pa;
    private int pollIndex = 0;
    private List<Integer> pListCopy;

    private boolean sx1Flag = false;
    private int lastBusnumber = 0;

    private static int leftover;
    private static boolean leftoverFlag = false;
    private static long lastReceived = 0;
    Boolean regFeedback = false;
    int regFeedbackAdr = 0;

    SXFCCInterface(String port) {
        this.portName = port;
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
            outputStream = serialPort.getOutputStream();
        } catch (IOException e) {
            System.out.println("Keinen Zugriff auf OutputStream");
        }

        try {
              inputStream = serialPort.getInputStream();
              while (inputStream.available() >= 1) {  // empty.
                    int b = inputStream.read();
                }
        } catch (IOException e) {
            System.out.println("Keinen Zugriff auf InputStream");
        }
        /*try {
            serialPort.addEventListener(new SXFCCInterface.serialPortEventListener());
        } catch (Exception e) {
            System.out.println("TooManyListenersException für Serialport");
        }
        serialPort.notifyOnDataAvailable(true); */

        try {
            serialPort.setSerialPortParams(baudrate, dataBits, stopBits, parity);
        } catch (UnsupportedCommOperationException e) {
            System.out.println("Konnte Schnittstellen-Paramter nicht setzen");
        }

        serialPortGeoeffnet = true;

        return true;
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
    }

    @Override
    public void doUpdate() {
        if (serialPortGeoeffnet) {
           /* try {
                while (inputStream.available() > 1) {
                    int b = inputStream.read();
                }
            } catch (IOException ex) {
                System.out.println("ERROR: Serial-IO where trying to empty inputstream");
            } */
            try {
                Byte[] b = {0x78, 0x03};
                outputStream.write(b[0]);
                outputStream.write(b[1]);
                outputStream.flush();
            } catch (IOException ex) {
                System.out.println("ERROR: Serial-IO where trying to write");
            }
            shortSleep();
            try {
                int count = 0;  // byte numbering in FCC-manual starts with 1 !
                // but for consistency with sxData array we start with 0 here
                while (inputStream.available() >= 1) {
                    int b = inputStream.read();
                    if (b == -1) {
                        break;
                    }
                    if (count < 110) {
                        sxData[count][0] = b & 0xff;                       
                    } else if (count == 112) {  
                        //System.out.println("power="+b);
                        if (b == 0) {
                            sxData[127][0] = 0;
                        } else {
                            sxData[127][0] = 80;
                        }                       
                    } else if (count < 226) {
                        //sxData[count-114][1] = b & 0xff;                           
                    } else if (count == 226) {               
                        if (b == 0) {
                           // sxData[127][1] = 0;
                        } else {
                           //  sxData[127][1] = 80;
                        }  
                    }
                    count++;
                }
                if (count != 226) {
                    System.out.println("ERROR wrong number of bytes read="+ count);
                }
            } catch (IOException ex) {
                System.out.println("ERROR: Serial-IO where trying to read");
            }
        }
    }

    private void shortSleep() {
        try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(SXFCCInterface.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
    
    @Override
    public void registerFeedback(int adr
    ) {
        // not necessary, because it is polled every second
    }

    @Override
    public void switchPowerOn() {

        Byte[] b = {(byte) 0x00, (byte) 0xFF, (byte) 0x01};

        try {
            outputStream.write(b[0]);
            outputStream.write(b[1]);
            outputStream.write(b[2]);
            outputStream.flush();
        } catch (IOException e) {
            System.out.println("Error: Serial Fehler beim Senden");
        }
shortSleep();
        try {
            inputStream.read();
        } catch (IOException ex) {
            System.out.println("Error: Serial Fehler beim Empfangen");
        }
    }

    @Override
    public void switchPowerOff() {

        Byte[] b = {(byte) 0x00, (byte) 0xFF, (byte) 0x00};

        try {
            outputStream.write(b[0]);
            outputStream.write(b[1]);
            outputStream.write(b[2]);
            outputStream.flush();
        } catch (IOException e) {
            System.out.println("Fehler beim Senden");
        }
        shortSleep();
        try {
            inputStream.read();
        } catch (IOException ex) {
            System.out.println("Error: Serial Fehler beim Empfangen");
        }

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

    @Override
    public void send(Byte[] data, int busnumber) {
        try {
            outputStream.write((byte) busnumber);
            outputStream.write(data[0]);
            outputStream.write(data[1]);
            outputStream.flush();
            // done via polling in LanbahnUI // doLanbahnUpdate((byte)(data[0] & 0x7f), data[1]);
        } catch (IOException e) {
            System.out.println("Fehler beim Senden");
        }
        shortSleep();
        try {
            int result = inputStream.read();
            if (result != 0) {
                System.out.println("Error: Serial Fehler beim Senden");
            }
        } catch (IOException ex) {
            System.out.println("Error: Serial Fehler beim Empfangen");
        }
    }

    
}
