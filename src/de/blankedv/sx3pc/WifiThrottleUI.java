/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import static de.blankedv.sx3pc.MainUI.*;
import javax.swing.text.BadLocationException;

/**
 *
 * @author mblank
 * 
 * Nov 2018 : Simplified - only for Funkr2 Throttles (and only receive multicast lanbahn messages)
 * 
 */
public class WifiThrottleUI extends javax.swing.JFrame {

    private static final long serialVersionUID = 5313123456436L;
    private static final int LANBAHN_PORT = 27027;
    private static final String LANBAHN_GROUP = "239.200.201.250";
    private static String TEXT_ENCODING = "UTF8";
    protected InetAddress mgroup;
    protected MulticastSocket multicastsocket;
    static LanbahnServer lbServer;

    // Preferences
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    protected Thread t;
    //protected RegisterJMDNSService serv;
    byte[] buf = new byte[1000];

    private long announceTimer = 0;

    /**
     * Creates new form LanbahnUI
     */
    public WifiThrottleUI() {
        initComponents();

        loadPrefs();

        if (!myip.isEmpty()) {
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

            //        Timer timer = new Timer();  // for sending multicast messages
            //        timer.schedule(new MCSendTask(), 1000, 1000);
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

                byte[] bytes = new byte[100];
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length);

                while (running) {
                    // Warten auf Nachricht
                    multicastsocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength(), TEXT_ENCODING);
                    message = message.replace("\n", ""); // .replace("  ", " ");
                    String ipAddr = packet.getAddress().toString().substring(1);
                    // don't react on "self" messages
                    //if (!isOwnIP(ipAddr)) {
                    String s;
                    try {
                        s = message + " (" + ipAddr + ")\n" + lanbahnTa.getText(0, 5000);
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
