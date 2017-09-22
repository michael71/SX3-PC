/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import static de.blankedv.sx3pc.InterfaceUI.*;
import java.util.ArrayList;
import javax.swing.text.BadLocationException;

/**
 *
 * @author mblank
 */
public class LanbahnUI extends javax.swing.JFrame {

    private static final long serialVersionUID = 5313123456436L;
    private static final int LANBAHN_PORT = 27027;
    private static final String LANBAHN_GROUP = "239.200.201.250";
    private static String TEXT_ENCODING = "UTF8";
    protected InetAddress mgroup;
    protected MulticastSocket multicastsocket;
    static LanbahnServer lbServer;

    private final int[][] sxDataCopy = new int[SXMAX2][NBUSSES];
    // Preferences
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    protected Thread t;
    //protected RegisterJMDNSService serv;
    byte[] buf = new byte[1000];

    private long announceTimer = 0;

    /**
     * Creates new form LanbahnUI
     */
    public LanbahnUI() {
        initComponents();

        loadPrefs();

        if (!myip.isEmpty()) {
            for (int ch = 0; ch < SXMAX2; ch++) {
                sxDataCopy[ch][0] = sxData[ch][0];
                sxDataCopy[ch][1] = sxData[ch][1];
            }
            try {
                multicastsocket = new MulticastSocket(LANBAHN_PORT);
                multicastsocket.setInterface(myip.get(0));
                mgroup = InetAddress.getByName(LANBAHN_GROUP);
                multicastsocket.joinGroup(mgroup);
                // s = new ServerSocket(SXNET_PORT,0,myip.get(0));  
                // only listen on 1 address on multi homed systems
                System.out.println("new lanbahn multicast socket " + multicastsocket.getInterface().toString() + ":" + LANBAHN_PORT);
                System.out.println("interface= " + multicastsocket.getNetworkInterface().toString());
                // DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), group, 6789);
            } catch (IOException ex) {
                System.out.println("could not open server socket on port=" + LANBAHN_PORT + " - closing lanbahn window.");
                JOptionPane.showMessageDialog(null, "could not open lanbahn socket!\n" + ex.toString(), "Error", JOptionPane.OK_CANCEL_OPTION);
                return;
            }
            startLanbahnServer();  // for receiving multicast messages

            Timer timer = new Timer();  // for sending multicast messages
            timer.schedule(new MCSendTask(), 1000, 1000);

            setVisible(true);
            //new Thread(new RegisterJMDNSService("lanbahn", LANBAHN_PORT, myip.get(0))).start();
        } else {
            System.out.println("no network adapter, cannot listen to lanbahn messages.");
        }
    }

    private void startLanbahnServer() {
        if (lbServer == null) {
            lbServer = new LanbahnServer();
            t = new Thread(lbServer);
            t.start();

        }

    }

    public void savePrefs() {
        // called when main prog is closing
        // fuer SX3 Programm, zB Belegtmelder: Instanz-Nummer (Klassenvariable) mit im
        // Pfad, um mehrere Fensterpositionen zu speichern
        // auch SX-adresse jeweils speichern.
        prefs.putInt("lanbahnwindowX", getX());
        prefs.putInt("lanbahnwindowY", getY());
    }

    private void loadPrefs() {
        setLocation(prefs.getInt("lanbahnwindowX", 170), prefs.getInt("lanbahnwindowY", 170));
    }

    class LanbahnServer implements Runnable {

        public void run() {
            try {

                byte[] bytes = new byte[65536];
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length);

                while (running) {
                    // Warten auf Nachricht
                    multicastsocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength(), TEXT_ENCODING);
                    message = message.replace("\n", "").replace("  ", " ");
                    String ipAddr = packet.getAddress().toString().substring(1);
                    // don't react on "self" messages
                    //if (!isOwnIP(ipAddr)) {
                    String s;
                    try {
                        s = message + " (" + ipAddr + ")\n" + lanbahnTa.getText(0,5000);
                    } catch (BadLocationException ex) {
                        s = message + " (" + ipAddr + ")\n" + lanbahnTa.getText();
                    }
                    lanbahnTa.setText(s);
                    //lanbahnTa.insert(message + " (" + ipAddr + ")\n", 0);
                    
                    interpretLanbahnMessage(message, ipAddr);
                    FunkreglerUI.setAliveByIP(ipAddr);
                    //}

                }
                System.out.println("lanbahn Server closing.");
                multicastsocket.leaveGroup(mgroup);
                multicastsocket.close();

            } catch (IOException ex) {
                System.out.println("lanbahnServer error:" + ex);
            }

        }

        private void interpretLanbahnMessage(String msg, String ip) {
            if (msg == null) {
                return;
            }
            int lbaddr, sxaddr, speed, data, lbdata;

            String cmd[] = msg.split(" ");

            switch (cmd[0].toUpperCase()) {
                case "LOCO":
                    if (cmd.length >= 6) {
                        try {
                            lbaddr = Integer.parseInt(cmd[1]);
                            if (!isValidLanbahnAddress(lbaddr)) {
                                // ignore invalid lanbahn addresses
                                return;
                            }
                            speed = Integer.parseInt(cmd[2]);
                            boolean forward = true;
                            if (cmd[3] == "1") {
                                forward = false;
                            }
                            boolean licht = false;
                            if (cmd[4] == "1") {
                                forward = true;
                            }
                            boolean horn = false;
                            if (cmd[5] == "1") {
                                forward = true;
                            }
                            sxaddr = lbaddr;  // TODO add mapping of loco addresses later
                            if (!isValidSXAddress(sxaddr)) {
                                // ignore invalid sx addresses
                                return;
                            }
                            sxi.sendLoco(sxaddr, speed, licht, forward, horn);
                        } catch (Exception e) {
                            System.out.println("could not understand LOCO command format: " + msg + " error=" + e.getMessage());
                        }
                    }
                    break;
                case "SX":
                    // selectrix command, no further processing of data byte
                    try {
                        if (cmd.length >= 3) {
                            sxaddr = Integer.parseInt(cmd[1]);
                            if (!isValidSXAddress(sxaddr)) {
                                // ignore invalid sx addresses
                                return;
                            }
                            data = Integer.parseInt(cmd[2]);
                            if (sxi != null) {
                                sxi.sendAccessory(sxaddr, data);
                            } else {
                                System.out.println("could not set SX, interface not connected");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("could not understand SX command format: " + msg + " error=" + e.getMessage());
                    }
                    break;
                case "S":  // standard lanbahn message for turnouts, signals etc
                case "SET":
                    // set command
                    try {
                        if (cmd.length >= 3) {
                            lbaddr = Integer.parseInt(cmd[1]);
                            if (!isValidLanbahnAddress(lbaddr)) {
                                // ignore invalid lanbahn addresses
                                return;
                            }
                            lbdata = Integer.parseInt(cmd[2]);
                            SXAddrAndBits sx = UtilityMapping.getSXAddrAndBitsFromLanbahnAddr(lbaddr);
                            if (DEBUG) {
                                System.out.println("lb-in: lbaddr=" + lbaddr + " val=" + lbdata);
                                System.out.println("sx: " + sx.toString());
                            }
                            if (sx.sxAddr == INVALID_INT) {
                                // pure lanbahn address range
                                lanbahnData.put(lbaddr, lbdata);
                            } else {
                                // selectrix channel range   

                                try_set_sx_accessory(sx, lbdata);

                            }

                        }
                    } catch (Exception e) {
                        System.out.println("could not understand S/SET command format: " + msg + " error=" + e.getMessage());
                    }
                    break;
                case "R":
                case "READ":
                    // set command
                    try {
                        if (cmd.length >= 2) {
                            lbaddr = Integer.parseInt(cmd[1]);
                            if (!isValidLanbahnAddress(lbaddr)) {
                                return;
                            }

                            String msgToSend = "";
                            SXAddrAndBits sx = UtilityMapping.getSXAddrAndBitsFromLanbahnAddr(lbaddr);
                            if (sx.sxAddr == INVALID_INT) {
                                if (!lanbahnData.containsKey(lbaddr)) {
                                    // initialize to "0" (=start simulation and init to "0")
                                    // if not already exists
                                    lanbahnData.put(lbaddr, 0);
                                }
                                // send lanbahnData, when already set
                                msgToSend = "FB " + lbaddr + " " + lanbahnData.get(lbaddr);

                            } else {
                                // selectrix channel range, get value from SX data array mapping
                                int dOut = INVALID_INT;
                                int d = sxData[sx.sxAddr][sxbusControl];
                                // check mapping for the following bit also
                                switch (sx.nbit) {
                                    case 1:
                                        dOut = (d >> (sx.bit - 1)) & 0x01;  // sxBit = 1...8
                                        break;
                                    case 2:
                                        dOut = (d >> (sx.bit - 1)) & 0x03;  // two consecutive bits
                                        break;
                                }
                                if (dOut != INVALID_INT) {
                                    msgToSend = "FB " + lbaddr + " " + dOut;
                                } else {
                                    System.out.println("ERROR, could not get SX Value");
                                }
                            }
                            if (!msgToSend.isEmpty()) {
                                byte[] buf = new byte[256];
                                buf = msgToSend.getBytes();
                                System.out.println("to lanbahn: " + msgToSend);
                                DatagramPacket packet;
                                packet = new DatagramPacket(buf, buf.length, mgroup, LANBAHN_PORT);

                                try {
                                    multicastsocket.send(packet);
                                } catch (IOException ex) {
                                    if (DEBUG) {
                                        System.out.println("could not send LB Msg");
                                    }
                                }
                            }

                        }
                    } catch (Exception e) {
                        System.out.println("could not understand R/READ command format: " + msg + " error=" + e.getMessage());
                    }
                    break;
                case "A":
                    // announcement of name / ip-address / battery-state
                    try {
                        if (cmd.length >= 4) {
                            String name = cmd[1];
                            if (name.contains("FUNKR")) {  // only "FUNKR" is a "lanbahn FREDI"

                                // check, if it is known already
                                if (FunkreglerUI.isKnown(name)) {
                                    FunkreglerUI.updateByName(name, cmd);
                                } else {
                                    FunkreglerUI fu1 = new FunkreglerUI(name, cmd);
                                }

                            }
                        }
                    } catch (Exception e) {
                        System.out.println("could not understand S command format: " + msg + " error=" + e.getMessage());
                    }
                    break;

            }

        }

        private boolean isOwnIP(String ip) {
            //System.out.println(ip);
            for (int i = 0; i < myip.size(); i++) {
                //System.out.println(myip.get(i).toString());
                if (myip.get(i).toString().contains(ip)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isValidLanbahnAddress(int addr) {
            if ((addr > 0) && (addr <= LBMAX)) {
                return true;
            } else {
                if (DEBUG) {
                    System.out.println("ERROR: lb-address=" + addr + " ignored.");
                }
                return false;
            }
        }

        private boolean isValidSXAddress(int addr) {
            if ((addr > 0) || (addr < SXMAX_USED)) {
                return true;
            } else {
                if (DEBUG) {
                    System.out.println("ERROR: sx-address=" + addr + " ignored.");
                }
                return false;
            }
        }

        private void try_set_sx_accessory(SXAddrAndBits sx, int data) {
            if (!sxi.isConnected()) {
                System.out.println("could not set SX, interface not connected");
                return;
            }
            if ((sx.bit >= 1) && (sx.bit <= 8) && (sx.nbit <= 4) && (sx.nbit >= 1)) {
                int d = sxData[sx.sxAddr][sxbusControl];
                switch (sx.nbit) {
                    case 1:
                        if (data > 1) {
                            System.out.println("could not set SX, data >1 (nbit = 1)");
                            return;
                        }
                        d = SXUtils.bitOperation(d, sx.bit, (data & 0x01));
                        break;
                    case 2:
                        if (data > 3) {
                            System.out.println("could not set SX, data >3 (nbit = 2)");
                            return;
                        }
                        d = SXUtils.bitOperation(d, sx.bit, (data & 0x01));
                        d = SXUtils.bitOperation(d, sx.bit + 1, (data & 0x02));
                        break;
                    case 3:
                        if (data > 7) {
                            System.out.println("could not set SX, data >7 (nbit = 3)");
                            return;
                        }
                        d = SXUtils.bitOperation(d, sx.bit, (data & 0x01));
                        d = SXUtils.bitOperation(d, sx.bit + 1, (data & 0x02));
                        d = SXUtils.bitOperation(d, sx.bit + 2, (data & 0x04));
                        break;
                    case 4:
                        if (data > 15) {
                            System.out.println("could not set SX, data >15 (nbit = 4)");
                            return;
                        }
                        d = SXUtils.bitOperation(d, sx.bit, (data & 0x01));
                        d = SXUtils.bitOperation(d, sx.bit + 1, (data & 0x02));
                        d = SXUtils.bitOperation(d, sx.bit + 2, (data & 0x04));
                        d = SXUtils.bitOperation(d, sx.bit + 2, (data & 0x08));
                        break;

                }
                sxi.sendAccessory(sx.sxAddr, d);  // set changed data value
                if (DEBUG) {
                    System.out.println("setting sx-adr=" + " val=" + d);
                }
            } else {
                if (DEBUG) {
                    System.out.println("could not set sx-adr=" + sx.sxAddr + " bit=" + sx.bit);
                }
            }
        }

    }

    /**
     * send command to lanbahn if the corresponding SX data have been changed
     */
    class MCSendTask extends TimerTask {

        public void run() {
            // send data to lanbahn when SX data have changed or when 
            // lanbahn data have changed
            int bus;  //TODO
            for (int ch = 0; ch < SXMAX2; ch++) {

                if (sxData[ch][0] != sxDataCopy[ch][0]) {
                    if (DEBUG && (ch == 70)) {
                        System.out.println("sxData=" + sxData[ch][0] + " ..Copy=" + sxDataCopy[ch][0]);
                    }
                    // get all mappings which changed SX-values
                    // iterate over lanbahnSXPairs to get affected lanbahn channels
                    for (LanbahnSXPair lbx : allLanbahnSXPairs) {
                        if (lbx.sxAddr == ch) {
                            if (DEBUG && (ch == 70)) {
                                System.out.println("lbx=" + lbx.toString());
                            }
                            int lbvalue = lbx.getLBValueFromSXByte( sxData[ch][0]);

                            if (DEBUG && (ch == 70)) {
                                System.out.println("setting lbaddr=" + lbx.lbAddr + " to data=" + lbvalue);
                            }
                            lanbahnData.put(lbx.lbAddr, lbvalue);
                            sendMCChannel(lbx.lbAddr, lbvalue);
                        }
                    }
                    sxDataCopy[ch][0] = sxData[ch][0];
                }
            }

            if (System.currentTimeMillis() > announceTimer) {
                sendMCAnnounce();
                announceTimer = System.currentTimeMillis() + 1000 * 10;   // every 10 secs
            }
            try {
                Thread.sleep(300);  // send update only every 300msecs
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void sendMCChannel(int ch, int d) {
            if (multicastsocket == null) {
                System.out.println("Error: multicast socket is NULL");
                return;
            }
            String msg = "FB " + ch + " " + d;

            byte[] buf = new byte[256];

            buf = msg.getBytes();

            DatagramPacket packet;
            packet = new DatagramPacket(buf, buf.length, mgroup, LANBAHN_PORT);

            System.out.println("to lanbahn: " + msg);
            try {
                multicastsocket.send(packet);
            } catch (IOException ex) {
                Logger.getLogger(LanbahnUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void sendMCAnnounce() {
            if (multicastsocket == null) {
                System.out.println("Error: multicast socket is NULL");
                return;
            }
            if (myip.isEmpty()) {
                return;
            }

            String msg = "A SX3PC " + myip.get(0).getHostAddress();

            byte[] buf = new byte[256];

            buf = msg.getBytes();

            DatagramPacket packet;
            packet = new DatagramPacket(buf, buf.length, mgroup, LANBAHN_PORT);

            // System.out.println("lanbahn " + msg);
            try {
                multicastsocket.send(packet);
            } catch (IOException ex) {
                Logger.getLogger(LanbahnUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1aa = new javax.swing.JLabel();
        jScrollPane1aa = new javax.swing.JScrollPane();
        lanbahnTa = new javax.swing.JTextArea();

        setTitle("Lanbahn Monitor");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jLabel1aa.setText("Messages:");

        lanbahnTa.setColumns(30);
        lanbahnTa.setRows(10);
        lanbahnTa.setAutoscrolls(false);
        jScrollPane1aa.setViewportView(lanbahnTa);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1aa, javax.swing.GroupLayout.DEFAULT_SIZE, 422, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1aa)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1aa)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1aa, javax.swing.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        savePrefs();
        System.out.println("lanbahn window closed.");
        setVisible(false);
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1aa;
    private javax.swing.JScrollPane jScrollPane1aa;
    private javax.swing.JTextArea lanbahnTa;
    // End of variables declaration//GEN-END:variables
}
