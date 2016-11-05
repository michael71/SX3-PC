/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import java.util.ArrayList;
import java.util.TimerTask;
import java.util.Timer;
import java.util.List;
import java.util.prefs.Preferences;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import static de.blankedv.sx3pc.InterfaceUI.*;

/**
 *
 * @author mblank
 */
/**
 * This class handles the client input for one server socket connection.
 */
class SRCPSession implements Runnable {

    // modes during connection
    static final int MODE_HANDSHAKE = 0;
    static final int MODE_INFO = 1;
    static final int MODE_COMMAND = 2;
    private int mode;
    // prepare final GO for modes
    static final int PREP_COMMAND = 1;
    static final int PREP_INFO = 0;  // defaul
    private int prep;
    private Socket incoming;
    static int session_counter = 0;
    private int session_id;
    // list of sensors, in case FB is initialized
    private List<Integer> sensors = new ArrayList<Integer>();
    private int [] sensorData;
    private boolean enableFeedbackInfo = false;
    private int timeElapsed;
    private long timeFB =0;
    private int feedbackBus;     // used for all messages reg. FB
    private int busnumber = 1;   // used for all messages reg. GA, GL, Power

    private boolean lastPower = false;

    protected PrintWriter out;
    
    private int sxbus = 0;  // use SX0

    /**
     * Constructs a handler.
     * @param i the incoming socket
     */
    public SRCPSession(Socket i) {
        incoming = i;
        mode = MODE_HANDSHAKE; // start with handshake
        prep = PREP_COMMAND;  // default
        session_id = ++session_counter;

    }

    public void run() {
        try {
            OutputStream outStream = incoming.getOutputStream();
            out = new PrintWriter(outStream, true /* autoFlush */);
            InputStream inStream = incoming.getInputStream();
            Scanner in = new Scanner(inStream);
            mode = MODE_HANDSHAKE;
            Timer timer = new Timer();
            timer.schedule  ( new Task(), 1000, 1000 );

            sendMessage("SX3-Server SRCP 0.82; SRCPOTHER 0.83; SRCPOTHER 0.84");  // welcome string
            while (in.hasNextLine()) {
                String msg = in.nextLine().trim().toUpperCase();
                if (msg.length() > 0) {
                    if (DEBUG) {
                        System.out.println("srcp read: " + msg);
                    }

                    // Ausgabe der POSIX Sekunden
                    StringBuilder res = new StringBuilder(srcpTimeStamp());
                    res.append(handleMessage(msg));
                    sendMessage(res.toString());
                    
                } else {
                    // ignore empty lines
                    if (DEBUG) {
                        System.out.println("srcp read empty line");
                    }
                }
                
            }
            SRCPServerUI.taClients.append("client disconnected " + incoming.getRemoteSocketAddress().toString() + "\n");
        } catch (IOException e) {
            System.out.println("SRCPServerHandler Error: " + e);
        }
        try {
            incoming.close();
        } catch (IOException ex) {
            System.out.println("SRCPServerHandler Error: " + ex);
        }

        System.out.println("Closing SRCPserverHandler\n");
    }

    private String handleMessage(String message) {

        String result = "OK";

        if (mode == MODE_HANDSHAKE) {
            result = handleHandshakeMode(message);

        } else if (mode == MODE_COMMAND) {
            result = handleCommand(message);
        } else if (mode == MODE_INFO) {
            // do something in info mode
        }

        return result;
    }

    private String buildSRCPLocoMsg(int sxDataByte) {

        StringBuilder info = new StringBuilder();
        if ((sxDataByte & 32) != 0) { // == selectrix backward
            info.append("0 ");  // SRCP backward
        } else {
            info.append("1 ");  // SRCP forward
        }

        int speed = (100 * (sxDataByte & 0x1F)) / 31;
        info.append(speed);
        info.append(" 100 ");

        if ((sxDataByte & 64) != 0) {
            info.append("1 ");  // licht
        } else {
            info.append("0 ");
        }

        if ((sxDataByte & 128) != 0) {
            info.append("1 ");  // horn
        } else {
            info.append("0 ");
        }
        info.append("0 0 0");  // default 5 functions ... TODO
        return info.toString();
    }

    private static String srcpTimeStamp() {
        // Ausgabe der POSIX Sekunden
        long now = System.currentTimeMillis();
        long secs = now / 1000;
        long msecs = now - secs * 1000;
        StringBuilder tStamp = new StringBuilder(secs + "." + msecs + " ");
        return tStamp.toString();
    }

    

    private String handleHandshakeMode(String message) {
        // initial handshake
        String result;
        if (message.contains("SET PROTOCOL")) {
            // no check for SRCP version !
            result = "201 OK PROTOCOL SRCP";
        } else if (message.contains("CONNECTIONMODE") && message.contains("SRCP")) {
            result = "OK";
            prep = PREP_COMMAND; // default, if no parameter given
            if (message.contains("INFO")) {
                prep = PREP_INFO;
            }
        } else if (message.contains("GO")) {
            result = "OK GO " + session_id;
            if (prep == PREP_INFO) {
                mode = MODE_INFO;
            } else {
                mode = MODE_COMMAND;
            }
        } else {
            result = "";
        }
        return result;
    }

    private String handleCommand(String m) {
        String[] param = m.split("\\s+");  // one or more whitespace
        String result = "";

        // listen only for the correct busnumber
        try {
            Integer bu = Integer.parseInt(param[1]);
            if (bu != busnumber) {
                result = "411 ERROR UNKNOWN BUS NUMBER";
                if (DEBUG) {
                    System.out.println("incorrect bus number: " + bu);
                }
                return result;
            }
        } catch (Exception e) {
            result = "419 ERROR TOO SHORT";
            return result;
        }


        if (m.contains("INIT") && m.contains("POWER")) {
            result = "OK";
            // power on and power off
        } else if (m.contains("SET") && m.contains("POWER")) {
            result = "OK";
            if (m.contains("ON")) {
                sxi.powerOn();
            } else { // then it must be off
                sxi.powerOff();
            }


        } else if (m.contains("GET") && m.contains("POWER")) {
            if (sx.powerIsOn()) {
                result = "100 INFO "+busnumber+" POWER ON";
            } else { 
                result = "100 INFO "+busnumber+" POWER OFF";
            }

        } else if (m.contains("GET") && m.contains("GL")) {

            // GET Loco info
            try {
                // funktioniert nur, wenn entweder der Rautenhaus Modus läuft oder 
                // der Zustand der Adresse "adr" bereits gepollt wird.
                Integer adr = Integer.parseInt(param[3]);
                if (sxi.is825() || sx.getpList().contains(adr)) {
                    StringBuilder sb = new StringBuilder("100 INFO " + param[1] + " GL " + adr + " ");
                    result = sb.append(buildSRCPLocoMsg(sxData[adr][sxbus])).toString();
                } else {
                    result = "416 ERROR NO DATA";
                }
            } catch (Exception e) {
                result = "419 ERROR TOO SHORT";
            }

        } else if (m.contains("GET") && m.contains("GL")) {

            // GET Loco info
            try {
                // funktioniert nur, wenn entweder der Rautenhaus Modus läuft oder 
                // der Zustand der Adresse "adr" bereits gepollt wird.
                Integer adr = Integer.parseInt(param[3]);
                if (sxi.is825() || sx.getpList().contains(adr)) {
                    StringBuilder sb = new StringBuilder("100 INFO " + param[1] + " GL " + adr + " ");
                    result = sb.append(buildSRCPLocoMsg(sxData[adr][sxbus])).toString();
                } else {
                    result = "416 ERROR NO DATA";
                }
            } catch (Exception e) {
                result = "419 ERROR TOO SHORT";
            }

        }      else if (m.contains("SET") && m.contains("GL")) {
            if (DEBUG) {
                System.out.println("loco set command: " + m);
            }
            result = locoSetCommand(param);
        } else if (m.contains("SET") && m.contains("GA")) {
            if (DEBUG) {
                System.out.println("accessory set command: " + m);
            }
            result = accSetCommand(param);

        } else if (m.contains("INIT") && m.contains("GL")) {
            // Init Loco
            try {
                Integer adr = Integer.parseInt(param[3]);
                sx.addToPlist(adr);
                result = "OK";
            } catch (Exception e) {
                result = "419 ERROR TOO SHORT";
            }
        } else if (m.contains("INIT") && m.contains("GA")) {
            result = initAccessory(param);
        } else if (m.contains("INIT") && m.contains("FB")) {
            result = initFeedback(param);
        } else if (m.contains("TERM") && m.contains("FB")) {
            result = termFeedback(param);
        } else if (m.contains("GET") && m.contains("FB")) {
            result = getFeedback(param);
        } else if (m.contains("TERM") && m.contains("GL")) {
            // TERM Loco, Gegenstück zu init.
            try {
                Integer adr = Integer.parseInt(param[3]);
                sx.removeFromPlist(adr);
                result = "OK";
            } catch (Exception e) {
                result = "419 ERROR TOO SHORT";
            }
        } else if (m.contains("TERM") && m.contains("SESSION")) {
            // TERM SESSION
            // TODO ???????????????????????????????
            result = "OK";
        } else if (m.contains("TERM") && m.contains("POWER")) {
            // TERM POWER
            // TODO ???????????????????????????????
            result = "OK";
        } else {
            result = "410 ERROR UNKNOWN COMMAND";
        }
        return result;
    }

    private String locoSetCommand(String[] param) {
        String result;
        Integer adr;
        try {
            // funktioniert nur, wenn entweder der Rautenhaus Modus läuft oder der Zustand der Adresse adr bereits gepollt wird.
            adr = Integer.parseInt(param[3]);
            Boolean horn;
            if (!sx.getpList().contains(adr)) {
                sx.addToPlist(adr);
            }
            // param[4] = direction
            // param[5] / param [6] = rel. speed
            // param[7] = light on off
            // param[8] = horn on off
            if (param.length < 9) {
                horn = false;
            } else {
                horn = (Integer.parseInt(param[8]) == 1);
            }
            Boolean forward = (Integer.parseInt(param[4]) == 1);
            Boolean light = (Integer.parseInt(param[7]) == 1);
            int speed = (31 * Integer.parseInt(param[5])) / Integer.parseInt(param[6]);
            sxi.sendLoco(adr, speed, light, forward, horn);
            result = "OK";

        } catch (Exception e) {
            result = "419 ERROR TOO SHORT";
        }
        return result;
    }

    private String initAccessory(String[] param) {
        Integer adr, accNum, bit;
        String result = "OK";
        // polling einschalten
        try {
            accNum = Integer.parseInt(param[3]);
            adr = accNum / 10;
            if (!sx.getpList().contains(adr)) {
                sx.addToPlist(adr);
            }
        } catch (NumberFormatException numberFormatException) {
            result = "412 ERROR WRONG VALUE FOR ACC ADDRESS";
        }
        return result;
    }

   

    private String accSetCommand(String[] param) {
        String result;
        Integer adr, accNum, bit;
        try {
            // funktioniert nur, wenn entweder der Rautenhaus Modus läuft
            // oder der Zustand der Adresse adr bereits gepollt wird.
            //  !!! weil sonst andere Bits nicht bekannt sind !!!!
            accNum = Integer.parseInt(param[3]);
            adr = accNum / 10;
            bit = (accNum - adr * 10);

            if (!sx.getpList().contains(adr)) {
                sx.addToPlist(adr);
                result = "444 ERROR not polled";
            }
            // param[4] not used (=port)
            // param[5] 0=off 1=on  => this is the output for this bit.

            sxi.sendAccessoryBit(adr, bit, Integer.parseInt(param[5]));
            result = "OK";

        } catch (Exception e) {
            result = "419 ERROR TOO SHORT";
        }
        return result;
    }
     
    private String initFeedback(String[] param) {
        StringBuilder result = new StringBuilder();
        try {
            feedbackBus = Integer.parseInt(param[1]);
            enableFeedbackInfo = true;
            result.append("101 INFO ");
            result.append(feedbackBus);
            result.append(" FB");
            if (DEBUG)  System.out.println("fb initialized.");
        } catch (Exception e) {
            result.append("419 ERROR TOO SHORT");
            return result.toString();
        }

        readInitialSensorData();
        // should ALL sensor data be sent after init ???


        SRCPServerUI.taClients.append("Feedback init.\n");

        return result.toString();
    }

    private String termFeedback(String[] param) {
        StringBuilder result = new StringBuilder();
        try {
            if (Integer.parseInt(param[1]) == feedbackBus) {
                enableFeedbackInfo = false;
                result.append("102 INFO ");
                result.append(feedbackBus);
                result.append(" FB");
                if (DEBUG)  System.out.println("fb terminated.");
                SRCPServerUI.taClients.append("Feedback terminated.\n");
            } else {
                result.append("412 WRONG VALUE");
            }

        } catch (Exception e) {
            result.append("419 ERROR TOO SHORT");
        }
        return result.toString();
    }

    private String getFeedback(String[] param) {
        // GET Feedback info
        StringBuilder result = new StringBuilder();
        try {
            // funktioniert nur, wenn entweder der Rautenhaus Modus läuft oder
            // der Zustand der Adresse "adr" bereits gepollt wird.
            int longadr = Integer.parseInt(param[3]);
            int bus = Integer.parseInt(param[1]);
            // calculate (sx)adr and (sx)bit from long "adr"
            int adr = longadr / 10;
            int bit = longadr - 10*adr;
            if ((sxi.is825() || sx.getpList().contains(adr)) && (bus == feedbackBus)) {
                result.append("100 INFO ");
                result.append(param[1]);
                result.append(" FB ");
                result.append(longadr);
                int mask = (1 << (bit - 1));
                if ((sxData[adr][sxbus] & mask) != 0) {
                    result.append(" 1");
                } else {
                    result.append(" 0");
                }

            } else {
                result.append("416 ERROR NO DATA");
            }
        } catch (Exception e) {
            result.append("419 ERROR TOO SHORT");
        }
        return result.toString();
    }
    
    private void checkFeedbackAndSendMsg() {

        for (int i=0; i<sensors.size() ; i++) {
            int adr=sensors.get(i);  // sensor address
            int data = sxData[adr][sxbus];  // actual data on that address
                                     // sensorData[i] = last data of that sensor
            for (int bit = 1; bit <= 8; bit++) {
                // check every single bit
                int mask = (1 << (bit - 1));
                // compare to stored data for that bit.
                if ((data & mask) != (sensorData[i] & mask)) {
                    StringBuilder res = new StringBuilder(srcpTimeStamp());
                    res.append("INFO ");
                    res.append(feedbackBus);
                    res.append(" FB ");
                    res.append(adr);
                    res.append(bit);

                    if ( (data & mask) == 0)  {
                        // send "0"
                        res.append(" 0");
                    } else {
                        res.append(" 1");
                    }
                    sendMessage(res.toString());
                }
            }
            sensorData[i]=data; // copy new value
        }

    }

    private void readInitialSensorData() {
        // read sensor list to be able to send info when a sensor changes
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        String sel = prefs.get("SensorList", null);
        String[] slist = sel.split(";");
        if (slist.length > 0 && !slist[0].isEmpty()) {
            sensorData = new int[slist.length];
            int adr;
            for (int i = 0; i < slist.length; i++) {
                adr = Integer.parseInt(slist[i]);
                sensors.add(adr);
                sensorData[i]=sxData[adr][sxbus];
            }
        }
    }

    private void sendMessage(String res) {
            out.println(res);
            out.flush();
            if (DEBUG) {
                System.out.println("srcp send:" + res);
            }
    }

    private void checkPowerStatusAndSend() {
        boolean pow = ((sxData[127][sxbus] & 0x80) != 0);

        if (pow != lastPower) // then power status has changed
        // send info message
        {
            lastPower = pow;

            StringBuilder res = new StringBuilder(srcpTimeStamp());
            res.append("INFO ");
            res.append(busnumber);
            res.append(" POWER ");
            if (pow == true) {
                res.append(" ON ");
            } else {
                res.append(" OFF ");
            }

            sendMessage(res.toString());

        }
    }


    class Task extends TimerTask {
        public void run() {
            if (enableFeedbackInfo) {
                checkFeedbackAndSendMsg();
            }
            checkPowerStatusAndSend();
        }
    }

}

