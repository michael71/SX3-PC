/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import java.util.LinkedList;
import java.util.List;
import javax.swing.SwingWorker;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import static de.blankedv.sx3pc.InterfaceUI.*;   // DAS SX interface.

/**
 *
 * @author mblank
 */
// TODO open/close serial port NICHT durchführen bei Simulation.
public class SXInterface {

    private boolean noPollingFlag;
    private String portName;
    private final boolean simulation;
    private int baudrate ;
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
    private PollingActivity pa;
    private int pollIndex = 0;
    private List<Integer> pListCopy;
    
    private boolean sx1Flag = false;
    private int lastBusnumber = 0;

    private static int leftover;
    private static boolean leftoverFlag = false;
    private static long lastReceived = 0;
    Boolean regFeedback = false;
    int regFeedbackAdr = 0;

    public SXInterface(boolean noPoll, String portName, int baud, boolean simulation) {

        this.noPollingFlag = noPoll;
        this.portName = portName;
        this.simulation = simulation;
        this.baudrate = baud;
    }

    boolean open() {
        if (simulation) {
            serialPortGeoeffnet = true;
            return true;
        }
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
            leftoverFlag=false;
            inputStream = serialPort.getInputStream();
        } catch (IOException e) {
            System.out.println("Keinen Zugriff auf InputStream");
        }
        try {
            serialPort.addEventListener(new serialPortEventListener());
        } catch (Exception e) {
            System.out.println("TooManyListenersException für Serialport");
        }
        serialPort.notifyOnDataAvailable(true);

        try {
            serialPort.setSerialPortParams(baudrate, dataBits, stopBits, parity);
        } catch (UnsupportedCommOperationException e) {
            System.out.println("Konnte Schnittstellen-Paramter nicht setzen");
        }

        serialPortGeoeffnet = true;
        lastBusnumber = 0;
        setInterfaceMode();

        if (!noPollingFlag) {
            startPolling();
        }
        return true;
    }

    public void close() {
        if (simulation) {
            return;
        }
        if (!noPollingFlag) {
            stopPolling();
        }
        if (serialPortGeoeffnet == true) {
            System.out.println("Schließe Serialport");
            serialPort.close();
            serialPortGeoeffnet = false;
        } else {
            System.out.println("Serialport bereits geschlossen");
        }
    }

    private synchronized void send(Byte[] data, int busnumber) {
        // darf nicht unterbrochen werden
        // TODO check if switch from SX0 to Sx1 is necessary or vice versa
        // ************************************** using 0 only
        if (simulation) {
            // only write commands in simulation
        	// convert "signed" byte to unsigned int.
        	// (byte is ALLWAYS signed in Java)
            sxData[data[0] & 0x7F][busnumber] = toUnsignedInt(data[1]);
            //sx.doUpdate();
            return;
        }

        if (serialPortGeoeffnet != true) {
            System.out.println("Fehler beim Senden, serial port nicht geöffnet und simul. nicht gesetzt");
            return;
        }

        // check if sent on same bus as last command
        if (busnumber != lastBusnumber) {
            // send bus-command on channel 126
            lastBusnumber = busnumber;
            setBus(busnumber);
        }
        // falls im Trix Mode und schreib befehl, dann daten in sxData speichern
        // TODO ??? if ((value & (1L << x)) != 0)
        if ((!noPollingFlag) && ((data[0] & 0x80) != 0)) {
            sxData[data[0] & 0x7f][busnumber] = data[1];
        }
        lastAdrSent = toUnsignedInt(data[0]) & 0x7f;  // wird nur fuer NICHT slx825 format gebraucht
        if ((data[0] & 0x80) != 0) {
            System.out.println("wr-Cmd: adr " + (toUnsignedInt(data[0]) & 0x7f) + " / data " + toUnsignedInt(data[1]));
        } else {
            System.out.println("rd-Cmd: adr " + (toUnsignedInt(data[0]) & 0x7f));
        }

        try {
            outputStream.write(data[0]);
            outputStream.write(data[1]);
            outputStream.flush();
        } catch (IOException e) {
            System.out.println("Fehler beim Senden");
        }
    }

    public void resetAll() {

        Byte[] b = {(byte) 0xFF, (byte) 0x00};
        for (int i=0; i<112 ; i++) {
            b[0]=(byte)(  (i& 0x7F) | 0x80);  // write command
            send(b,0);
            try {
            Thread.sleep(50);
            } catch (Exception e) {};
        };
  
        if (useSX1forControl) {
            for (int i = 0; i < 112; i++) {
                b[0] = (byte) ((i & 0x7F) | 0x80);  // write command
                send(b, 1);
                try {
                    Thread.sleep(50);
                } catch (Exception e) {
                };
            };
        }
    }

    public void setInterfaceMode() {
        Byte[] b = {(byte) 0xFF, (byte) 0xFF};
        /* Falls SLX825:
        Über einen Schreibbefehl auf die Adresse 126 (=0xFE schreiben, =0x7E lesen
        können folgende Funktionen angewählt werden:

        Bit 7 = 1 (128)
        Überwachung „Ein“
        Hiermit wird das Rautenhaus-Befehlsformat eingeschaltet
        Diese Ausgabe löst jedes Mal den einmaligen Transfer der gesamten Datenbusinformation vom Interface
        zum Computer aus. Jede Änderung auf dem Datenbus wird automatisch sofort nach Erkennen an den Rechner
        geschickt. Im ersten Byte steht die Adresse, im zweiten Byte das zugehörige Datenwort. Das oberste Bit
        im Adressbyte kennzeichnet den Datenbus, bei dem die Änderung auftrat. Beim SLX825 mit nur einem
        Datenbus ist das Bit immer 0.

        Bit 6 = 1 (64)
        Überwachung „Aus“
        Das Rautenhaus-Befehlsformat wird ausgeschaltet.

        Bit 5 = 1 (32)
        Feedback „Ein“
        Bei Überwachung „Ein“ wird auch dann eine Änderung übermittelt, wenn die Änderung vom Rechner
        selbst über eine Ausgabe an das Interface ausgelöst wurde.

        Bit 4 = 1 (16)
        Feedback „Aus“

        Die Lesebefehle unterscheiden sich nicht von den Lesebefehlen im Trix-Format.
        * ******************************************************************
        * Falls ZS1/ZS" ebenfalls 128+32 setzen
         * */

        if (noPollingFlag) {
            b[0] = (byte) 0xFE; // Rautenhaus ein
            b[1] = (byte) 0xA0; // Feedback ein
              // wird also immer im Echo-Modus (Feedback) betrieben.
        } else {
            b[0] = (byte) 0xFE; // Rautenhaus aus
            b[1] = (byte) 0x48; // Feedback aus
        }

        try {
            outputStream.write(b[0]);
            outputStream.write(b[1]);
            outputStream.flush();
        } catch (IOException e) {
            System.out.println("Fehler beim Senden");
        }
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            Logger.getLogger(SXInterface.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
     public void setBus(int busnumber) {
        System.out.println("bus="+busnumber);
        Byte[] b = {(byte) 0xFE, (byte) 0x00};   // == Kanel 126
 
        if (busnumber == 1) {
             b[1] = (byte) 0x01; 
        }

        try {
            outputStream.write(b[0]);
            outputStream.write(b[1]);
            outputStream.flush();
        } catch (IOException e) {
            System.out.println("Fehler beim Senden");
        }
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            Logger.getLogger(SXInterface.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public synchronized void powerOff() {
        // 127 (ZE ein/aus) +128(schreiben) = 0xFF
        if (simulation) {
            sxData[127][0] = 0x00;
            return;
        }
        
        Byte[] b = {(byte) 0xFF, (byte) 0x00};
        try { outputStream.write(b[0]);
            outputStream.write(b[1]);
            outputStream.flush();
            } catch (IOException e) {
            System.out.println("Fehler beim Senden");
        }
    }

    public synchronized void powerOn() {
        // 127 (ZE ein/aus) +128(schreiben) = 0xFF
        if (simulation) {
            sxData[127][0] = 0x80;
            return;
        }
        Byte[] b = {(byte) 0xFF, (byte) 0x80};
        try { outputStream.write(b[0]);
            outputStream.write(b[1]);
            outputStream.flush();
            } catch (IOException e) {
            System.out.println("Fehler beim Senden");
        }
    }

    synchronized void sendLoco(int lok_adr, int speed, boolean licht, boolean forward, boolean horn) {
        // constructs SX loco data from speed and bit input.
        int data = 0;
 
        if (speed > 31) {
            speed = 31;
        }
        if (speed < 0) {
            speed = 0;
        }
        if (DEBUG) System.out.println("adr:" + lok_adr + " s:" + speed + " l:" + licht + " forw:" + forward + " h:" + horn);
        data = speed;  // die unteren 5 bits (0..4)
        if (horn) {
            data += 128; // bit7
        }
        if (licht) {
            data += 64; // bit6
        }
        if (forward == false) {
            data += 32; //bit5
        }
        if (DEBUG) System.out.println("update loco " + Integer.toHexString(data));
        Byte[] b = {(byte) (lok_adr + 128), 0};  // bit 7 muss gesetzt sein zum Schreiben
        b[1] = (byte) data;
        send(b,0);
    }

    synchronized void sendAccessoryBit(int adr, int bit, int data) {
        int d = sxData[adr][sxbusControl];
        Byte[] b = {(byte) (adr + 128), 0};  // bit 7 muss gesetzt sein zum Schreiben
        if (data == 1) {  // set bit
            d |= (1 << (bit-1));  // sx bit von 1 bis 8
        } else {
            // reset bit
            d = d & ~(1 << (bit-1));  // sx bit von 1 bis 8
        }
        b[1] = (byte) (d);
        send(b,sxbusControl);
    }
    
    synchronized void send2SXBusses(int adr, int data) {
        // accepts adresses >127 and then sends data to SX1 (instead of SX0)
        // locos always control on SX0, "schalten/melden" on SX0 or SX1
         if (adr > SXMAX2) {
            if (useSX1forControl == true) {
                adr = adr - 128;  // to make channel number clear.
                Byte[] b = {(byte) (adr + 128), (byte) data};  // bit 7 muss gesetzt sein zum Schreiben
                send(b, 1);
            } else {
                System.out.println("ERROR, trying to send to channel=" + adr + ", but only one SX Bus enabled.");
            }
        } else {
            Byte[] b = {(byte) (adr + 128), (byte) data};  // bit 7 muss gesetzt sein zum Schreiben
            send(b, 0);
        }
    }
    
    synchronized void send2SXBussesBit(int adr, int bit, int data) {
        // accepts adresses >127 and then sends data to SX1 (instead of SX0)
        //  sxData[adr];
        // twoBusses = false;
        // locos always control on SX0, "schalten/melden" on SX0 or SX1
        // static int sxbusControl = 0;
        int bus = 0;
        if (adr > SXMAX2) {
            if (useSX1forControl == true) {
                bus = 1;
                adr = adr - 128;
            } else {
                System.out.println("ERROR, trying to send to channel=" + adr + ", but only one SX Bus enabled.");
            }
        }
        int d = sxData[adr][bus];
        Byte[] b = {(byte) (adr + 128), 0};  // bit 7 muss gesetzt sein zum Schreiben
        if (data == 1) {  // set bit
            d |= (1 << (bit-1));  // sx bit von 1 bis 8
        } else {
            // reset bit
            d = d & ~(1 << (bit-1));  // sx bit von 1 bis 8
        }
        b[1] = (byte) (d);
        send(b,bus);
    
    }
     
    synchronized void sendAccessory(int adr, int data) {
      //  sxData[adr];
        Byte[] b = {(byte) (adr + 128), (byte) data};  // bit 7 muss gesetzt sein zum Schreiben
        send(b,sxbusControl);
    }
    
    public void readPower() {
        Byte[] b = {(byte) 127, (byte) 0x00};   // read power state
        send(b,0);
    }
    public boolean is825() {
        return noPollingFlag;
    }

    boolean getPowerState() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void registerFeedback(int sensorAdr) {
        regFeedbackAdr = sensorAdr;
        regFeedback = true;
    }

    public void unregisterFeedback() {
        regFeedback = false;
    }
    class serialPortEventListener implements SerialPortEventListener {

        public void serialEvent(SerialPortEvent event) {
            switch (event.getEventType()) {
                case SerialPortEvent.DATA_AVAILABLE:
                    connectionOK = true;
                    if (noPollingFlag) {
                        readSerialPortWriteToSX();
                    } else {
                        readSerialPortStandard();
                    }
                    break;
                case SerialPortEvent.BI:
                case SerialPortEvent.CD:
                case SerialPortEvent.CTS:
                case SerialPortEvent.DSR:
                case SerialPortEvent.FE:
                case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                case SerialPortEvent.PE:
                case SerialPortEvent.RI:
                default:
            }
        }
    }

    void readSerialPortWriteToSX() {

        // wird immer im Feedback(Echo)-Modus betrieben, das heisst
        // sxData wird nur hier geschrieben.
        // TODO achtung: immer auf 2 Byte warten .... timer reset wenn länger als 10 ms keine Bytes

        try {
           int adr, data;
 

   /*      case SerialPortEvent.DATA_AVAILABLE:
           readBuffer = new byte[8];
           try {
                while (inputStream.available()>0) {
                int numBytes = inputStream.read(readBuffer);
                * */
            byte[] readBuffer = new byte[60];
            
            while (inputStream.available() > 1) {
                int numBytes = inputStream.read(readBuffer);
                if (DEBUG) {System.out.println("read n="+numBytes); }
                int offset=0;
                if (leftoverFlag) {
                    offset = 1;
                    data=(int)(readBuffer[0] & 0xFF);
                    setSX(leftover,data);
                } else {
                    offset = 0;
                }
                for (int i=offset; i<numBytes; i=i+2) {
                    adr = (int)(readBuffer[0+i] & 0xFF);
                    if ((i+1) <numBytes) {
                       data = (int)(readBuffer[1+i] & 0xFF);
                       setSX(adr,data);
                       leftoverFlag = false;
                    } else {
                        // leftover data, no even number of data sent
                        // use next time
                        leftover = adr;
                        leftoverFlag = true; 
                    }
                }
                
            }
            //sx.doUpdate(); // nur im Rautenhaus Format nach jeder Info
            
            if (DEBUG) {
                System.out.println(".");
            }

        } catch (IOException e) {
            System.out.println("Fehler beim Lesen empfangener Daten");
        }

    }

    // address range 0 ..127 / 128 ... 255 / 256 ... 384
    private void setSX(int adr, int data) {
        if (adr >= 0 && adr < SXMAX2) {
            // data for SX0 bus
            sxData[adr][0] = data;
            if ((regFeedback) && (sxbusControl == 0)
                    && (adr == regFeedbackAdr)) {
                vtest.sensorFeedback(data);
            }
            if (DEBUG) System.out.print("SX0[" + adr + "]=" + data + " ");
        } else if (adr >= SXMAX2 && adr < (2*SXMAX2)) {
            // data for SX1 bus
            sxData[adr - 128][1] = data;
            if ((regFeedback) && (sxbusControl == 1)
                    && (adr == regFeedbackAdr)) {
                vtest.sensorFeedback(data);
            }
            if (DEBUG) System.out.print("SX1[" + (adr-SXMAX2) + "]=" + data + " ");
        } else if ((adr >= (2 * SXMAX2 )) && (adr < (3* SXMAX2)) ) {
            // data for SIM bus
            sxData[adr - 2*SXMAX2][2] = data;  // set only sxData[][2]
            if (DEBUG) System.out.print("SIM[" + (adr-2*SXMAX2) + "]=" + data + " ");
        }
    }
    
    
    void readSerialPortStandard() {
        // nur jeweils 1 Zeichen wird ausgewertet, die anderen werden ignoriert,
        // da nicht klar ist zu welcher Adresse sie gehören.
        // immer einzelnen bus = SX0
        if (DEBUG) {
            System.out.print("Sread: ");
        }
        try {
            int num;
            while (inputStream.available() > 0) {
                num = inputStream.read();
                if (DEBUG) {
                    System.out.print(num + " ");
                }
                // trix format, nur daten und nur nach anforderung (lastAdrSent)
                if (lastAdrSent != -1) {  // trix format, nur daten
                    if (DEBUG) {
                        System.out.print(" (adr=" + lastAdrSent + ")");
                    }
                    sxData[lastAdrSent][0] = num;  
                    lastAdrSent = -1;
                } else {
                    System.out.println(" (ignoriert)");
                }
            }
            //        System.out.println(".");
        } catch (IOException e) {
            System.out.println("Fehler beim Lesen empfangener Daten");
        }


    }

    public static int toUnsignedInt(byte value) {
        return (value & 0x7F) + (value < 0 ? 128 : 0);
    }

    public void pollNextAdr() {
        // wird nur für 66824 Interface verwendet.
        if ((pListCopy == null) || (pollIndex >= pListCopy.size())) {
            // (wieder) am Anfang, erzeuge Kopie der PollingListe
            pListCopy = new LinkedList<Integer>(sx.getpList());
            pollIndex = 0;
        }
        if (!pListCopy.isEmpty()) {
            if (DEBUG) {
                System.out.println("polling adr:" + pListCopy.get(pollIndex));
            }
            Byte[] b = {(byte) (pListCopy.get(pollIndex) + 0), 0};
            sxi.send(b,0);
            pollIndex++;
        } else {
            System.out.println("pList leer.");
        }
    }

    public void startPolling() {
        pListCopy = null; // to (re-)init pollNextAdr()
        pa = new PollingActivity();
        pa.execute();
    }

    public void stopPolling() {
    	if (pa != null) pa.cancel(true);

    }


    class PollingActivity extends SwingWorker<Void, Void> {

        /**
         * Polling is used to get info about SX Bus from old trix interface every 100ms
         *
         */
        protected Void doInBackground() throws Exception {
            try {
                while (true) {
                    Thread.sleep(100);
                    publish();    // ruft process() auf im UI Thread.
                }
            } catch (InterruptedException e) {
            }
            return null;
        }

        protected void process() {
            pollNextAdr(); // do in UI thread
        }
//        protected void done() {
//            // what needs to be done if DONE??
//        }
    }
}
