/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import static de.blankedv.sx3pc.InterfaceUI.*;
import java.awt.Color;

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

    private final int[][] sxDataCopy = new int[SXMAX2 + 1][2];
    // Preferences
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    protected Thread t;
    protected RegisterJMDNSService serv;
    byte[] buf = new byte[1000];

    private String dev1 = "";
    private String dev2 = "";

    /**
     * Creates new form LanbahnUI
     */
    public LanbahnUI() {
        initComponents();
        //progBattery1.setStringPainted(true);
        progBattery1.setForeground(Color.blue);
        progBattery2.setForeground(Color.green);

        loadPrefs();

        List<InetAddress> myip = NIC.getmyip();   // only the first one will be used
        if (!myip.isEmpty()) {
            for (int ch = 0; ch < SXMAX2 + 1; ch++) {
                sxDataCopy[ch][0] = sxData[ch][0];
                sxDataCopy[ch][1] = sxData[ch][1];
            }
            try {
                multicastsocket = new MulticastSocket(LANBAHN_PORT);
                mgroup = InetAddress.getByName(LANBAHN_GROUP);
                multicastsocket.joinGroup(mgroup);
                // s = new ServerSocket(SXNET_PORT,0,myip.get(0));  
                // only listen on 1 address on multi homed systems
                System.out.println("new lanbahn multicast socket " + myip.get(0) + ":" + LANBAHN_PORT);
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
            new Thread(new RegisterJMDNSService("lanbahn", LANBAHN_PORT, myip.get(0), this)).start();
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

                    message = message.replace("\n", "");
                    String ipAddr = packet.getAddress().toString().substring(1);
                    lanbahnTa.insert(message + " (" + ipAddr + ")\n", 0);
                    //lanbahnTa.append(message + "\n");
                    lbMessage2SX(message, ipAddr);

                }
                System.out.println("lanbahn Server closing.");
                multicastsocket.leaveGroup(mgroup);
                multicastsocket.close();

            } catch (IOException ex) {
                System.out.println("lanbahnServer error:" + ex);
            }

        }

        private void lbMessage2SX(String msg, String ip) {
            if (msg == null) {
                return;
            }
            int adr, speed, data;

            String cmd[] = msg.split(" ");

            switch (cmd[0]) {
                case "LOCO":
                    if (cmd.length >= 6) {
                        try {
                            adr = Integer.parseInt(cmd[1]);
                            if ((adr <= 0) || (adr > SXMAX_USED)) {
                                return;   // check address range
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
                            sxi.sendLoco(adr, speed, licht, forward, horn);
                        } catch (Exception e) {
                            System.out.println("could not understand LOCO command format: " + msg + " error=" + e.getMessage());
                        }
                    }
                    break;
                case "S":
                    // selectrix command
                    try {
                        if (cmd.length >= 3) {
                            adr = Integer.parseInt(cmd[1]);
                            if ((adr <= 0) || (adr > SXMAX_USED)) {
                                return;  // check address range
                            }
                            data = Integer.parseInt(cmd[2]);
                            sxi.sendAccessory(adr, data);
                        }
                    } catch (Exception e) {
                        System.out.println("could not understand S command format: " + msg + " error=" + e.getMessage());
                    }
                    break;
                case "A":
                    // announcement of name / ip-address / battery-state
                    try {
                        if (cmd.length >= 4) {
                            String name = cmd[1];
                            if (name.contains("FUNKR")) {  // only "FUNKR" is a "lanbahn FREDI"
                                
                            String ipDevice = cmd[2];
                            int batt = Integer.parseInt(cmd[3]);
                            int rssi = Integer.parseInt(cmd[4]);
                            if (dev1.isEmpty()) {
                                dev1 = name;
                                lblDevice1Info.setText(dev1);
                                setBatteryDisplay(progBattery1, batt);
                                setRSSIDisplay(progRSSI1, rssi);
                            } else if (dev1.equalsIgnoreCase(name)) {
                                setBatteryDisplay(progBattery1, batt);  // update battery status
                                setRSSIDisplay(progRSSI1, rssi);
                            } else if (dev2.isEmpty()) {
                                dev2 = name;
                                lblDevice2Info.setText(dev2);
                                setRSSIDisplay(progRSSI2, rssi);
                                setBatteryDisplay(progBattery2, batt);
                            } else if (dev2.equalsIgnoreCase(name)) {
                                setBatteryDisplay(progBattery2, batt); // update battery status
                                setRSSIDisplay(progRSSI2, rssi);
                            }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("could not understand S command format: " + msg + " error=" + e.getMessage());
                    }
                    break;

            }

        }

        private void setBatteryDisplay(javax.swing.JProgressBar p, int batt) {
            p.setValue(batt);
            if (batt < 3700) {
                p.setForeground(Color.red);
            } else if (batt < 3900) {
                p.setForeground(Color.yellow);
            } else {
                p.setForeground(Color.green);
            }
        }
        
        private void setRSSIDisplay(javax.swing.JProgressBar p, int rssi) {
             p.setValue(rssi);
            if (rssi < -75) {
                p.setForeground(Color.red);
            } else if (rssi < -70) {
                p.setForeground(Color.yellow);
            } else {
                p.setForeground(Color.green);
            }
        }
    }

    class MCSendTask extends TimerTask {

        public void run() {
            int bus;
            for (int ch = 0; ch < SXMAX2 + 1; ch++) {
                if (sxData[ch][0] != sxDataCopy[ch][0]) {
                    // channel data changed, send update to mobile device
                    sxDataCopy[ch][0] = sxData[ch][0];
                    //System.out.println("X " + i + " " + sxDataCopy[ch][bus]);
                    sendMCChannel(ch, 0, sxDataCopy[ch][0]);
                    //ms.send(packet); //sendMessage("X " + i + " " + sxDataCopy[ch][bus]);
                }

            }
            try {
                Thread.sleep(300);  // send update only every 300msecs
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void sendMCChannel(int ch, int bus, int d) {
            if (multicastsocket == null) {
                System.out.println("Error: multicast socket is NULL");
                return;
            }
            String msg = "S " + ch + " " + d;

            byte[] buf = new byte[256];

            buf = msg.getBytes();

            DatagramPacket packet;
            packet = new DatagramPacket(buf, buf.length, mgroup, LANBAHN_PORT);

            System.out.println("lanbahn " + msg);
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
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        lblDevice1Info = new javax.swing.JLabel();
        progBattery1 = new javax.swing.JProgressBar();
        lblDevice2Info = new javax.swing.JLabel();
        progBattery2 = new javax.swing.JProgressBar();
        progRSSI1 = new javax.swing.JProgressBar();
        progRSSI2 = new javax.swing.JProgressBar();

        setTitle("Lanbahn Monitor");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jLabel1aa.setText("Messages:");

        lanbahnTa.setColumns(20);
        lanbahnTa.setRows(5);
        jScrollPane1aa.setViewportView(lanbahnTa);

        jLabel1.setText("Device1");

        jLabel2.setText("Battery1");

        jLabel3.setText("Device2");

        jLabel4.setText("Battery2");

        lblDevice1Info.setText("-");

        progBattery1.setMaximum(4400);
        progBattery1.setMinimum(3300);

        lblDevice2Info.setText("-");

        progBattery2.setMaximum(4400);
        progBattery2.setMinimum(3300);

        progRSSI1.setMaximum(-40);
        progRSSI1.setMinimum(-80);
        progRSSI1.setOrientation(1);

        progRSSI2.setMaximum(-40);
        progRSSI2.setMinimum(-80);
        progRSSI2.setOrientation(1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1aa)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(lblDevice1Info, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1aa)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addComponent(jLabel3)
                                        .addGap(17, 17, 17)
                                        .addComponent(lblDevice2Info, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(128, 128, 128))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel4)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(progBattery2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel2)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(progBattery1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(progRSSI2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(progRSSI1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(lblDevice1Info))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(progBattery1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2)))
                    .addComponent(progRSSI1, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(progRSSI2, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addComponent(lblDevice2Info)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(progBattery2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 51, Short.MAX_VALUE)
                .addComponent(jLabel1aa)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1aa, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        savePrefs();
        System.out.println("lanbahn window closed.");
        setVisible(false);
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel1aa;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1aa;
    private javax.swing.JTextArea lanbahnTa;
    private javax.swing.JLabel lblDevice1Info;
    private javax.swing.JLabel lblDevice2Info;
    private javax.swing.JProgressBar progBattery1;
    private javax.swing.JProgressBar progBattery2;
    private javax.swing.JProgressBar progRSSI1;
    private javax.swing.JProgressBar progRSSI2;
    // End of variables declaration//GEN-END:variables
}
