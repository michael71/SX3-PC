/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.MainUI.SXMAX;
import static de.blankedv.sx3pc.MainUI.connectionOK;
import static de.blankedv.sx3pc.MainUI.sxData;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
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

    private final int baudrate = 230400;   // fix for FCC

    CommPortIdentifier serialPortId;
    Enumeration enumComm;
    SerialPort serialPort;
    OutputStream outputStream;
    InputStream inputStream;
    Boolean serialPortGeoeffnet = false;

    Boolean regFeedback = false;
    int regFeedbackAdr = 0;

    private static int fccErrorCount = 0;

    SXFCCInterface(String port) {
        this.portName = port;
    }

    public void setPort(String port) {
        portName = port;
    }

    public String getPortName() {
        return portName;
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
            serialPort.setSerialPortParams(baudrate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        } catch (UnsupportedCommOperationException e) {
            System.out.println("Konnte Schnittstellen-Paramter nicht setzen");
        }

        serialPortGeoeffnet = true;
        connected = true;

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
        fccErrorCount = 0; // reset errors
        connected = false;
    }

    @Override
    public synchronized String  doUpdate() {

        if (fccErrorCount > 10) {
            System.out.println("ERROR: FCC does not respond");
            return ("ERROR: Keine Response von der FCC, SerialPort Settings überprüfen");

        }
        if (serialPortGeoeffnet) {
            try {  // empty input
                while (inputStream.available() >= 1) {
                    int b = inputStream.read();
                }
            } catch (IOException ex) {
                ;
            }
            try {
                // request block of SX0 / SX1 bus data
                Byte[] b = {0x78, 0x03};
                outputStream.write(b[0]);
                outputStream.write(b[1]);
                outputStream.flush();
            } catch (IOException ex) {
                System.out.println("ERROR: Serial-IO where trying to write");
                fccErrorCount++;
            }
            shortSleep();  // must wait 40 milliseconds to have all 226 channels received
            try {
                //int count = 0;  // byte numbering in FCC-manual starts with 1 !
                // but for consistency with sxData array we start with 0 here
                
                byte[] buf = new byte[226];  // in case w
                int nread = inputStream.read(buf, 0, 226);
                
                if (nread != 226) {
                    System.out.println("ERROR wrong number of bytes read=" + nread);
                    fccErrorCount++;
                    return "ERROR";
                } else {
                    fccErrorCount = 0;
                    //System.out.println("226 bytes gelesen");
                }  
                    
                for (int count =0; count < 226; count++) {
                    
                    if (count < SXMAX) {
                        sxData.set(count,(buf[count] & 0xff));
                    } else if (count == 112) {
                        //System.out.println("power="+buf[count]);
                        if (buf[count] == 0) {
                            sxData.set(127,0);
                        } else {
                            sxData.set(127,0x80);
                        }
                        //System.out.println("sxData[127]="+sxData.get(127));
                    } // ignore SX1 data
      
                }
                
            } catch (IOException ex) {
                System.out.println("ERROR: Serial-IO where trying to read");
                fccErrorCount++;
            }
        }

        return "";
    }

    private void shortSleep() {
        try {
            Thread.sleep(40);
        } catch (InterruptedException ex) {
            Logger.getLogger(SXFCCInterface.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String getMode() {
        if (!connected) return "-";
        // siehe FCC Interface Manual, Seite 7
        switch (sxData.get(110) & 0x0f) {
            case 0x00:
                return "nur SX1";
            case 0x02:
                return "SX1+SX2";
            case 0x04:
                return "SX1,SX2,DCC";
            case 0x05:     
                return "SX1 + SX2 + MM";
            case 0x06:
                return "nur DCC";              
            case 0x07:
                return "nur MM";
            case 0x0b:
                return "SX1,SX2,DCC,MM";
            default:
                return "check FCC mode";           
        }
      }
    
    @Override
    public void registerFeedback(int adr
    ) {
        // not necessary, because it is polled every second
    }

    // für alle Schreibbefehle and die FCC muss zusätzlich zur Kanalnummer
    // das höchste Bit auf 1 gesetzt werden
    //Gleisspannung ein (SX1/2-Bus 0):
    //Vom PC: 0x00 0xFF Ungleich 0x00 Zum PC: 0x00
    //Gleisspannung aus (SX1/2-Bus 0):
    //Vom PC: 0x00 0xFF Gleich 0x00 Zum PC: 0x00
    @Override
    public synchronized void switchPowerOn() {
        System.out.println("FCC: switchPowerOn");
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
    public synchronized void switchPowerOff() {
        System.out.println("FCC: switchPowerOff");
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
        return; // "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

    /** für alle Schreibbefehle and die FCC muss zusätzlich zur Kanalnummer
    * das höchste Bit auf 1 gesetzt werden
    * * TODO check FCC */
    @Override
    public synchronized boolean send(Byte[] data) {
        try {
            outputStream.write((byte)0);  // TODO ????
            outputStream.write(data[0]);
            outputStream.write(data[1]);
            outputStream.flush();
            // done via polling in LanbahnUI // doLanbahnUpdate((byte)(data[0] & 0x7f), data[1]);
        } catch (IOException e) {
            System.out.println("Fehler beim Senden");
            return false;
        }
        shortSleep();
        // quittung abwarten
        try {
            int result = inputStream.read();
            if (result != 0) {
                System.out.println("Error: Serial Fehler beim Senden");
                return false;
            } else {
                return true;
            }
        } catch (IOException ex) {
            System.out.println("Error: Serial Fehler beim Empfangen");
            return false;
        }
        
    }

}
