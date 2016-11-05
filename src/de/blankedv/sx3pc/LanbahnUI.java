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
import static de.blankedv.sx3pc.InterfaceUI.SXMAX2;
import static de.blankedv.sx3pc.InterfaceUI.running;
import static de.blankedv.sx3pc.InterfaceUI.sxData;
import static de.blankedv.sx3pc.InterfaceUI.DEBUG;
import static de.blankedv.sx3pc.InterfaceUI.sxi;
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

    /**
     * Creates new form LanbahnUI
     */
    public LanbahnUI() {
        initComponents();
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

    public void displayMessage(String s) {
        lanbahnTa.append(s + "\"");
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
                    lanbahnTa.append(message + "\n");
                    lbMessage2SX(message);

                }
                System.out.println("lanbahn Server closing.");
                multicastsocket.leaveGroup(mgroup);
                multicastsocket.close();

            } catch (IOException ex) {
                System.out.println("lanbahnServer error:" + ex);
            }

        }
        
        private void lbMessage2SX(String msg) {
            if (msg == null) return;
            
            String cmd[] = msg.split(" ");
            if (cmd.length < 6)  return;
            switch (cmd[0]) {
                case "LOCO":
                    try {
                    int adr = Integer.parseInt(cmd[1]);
                    int speed = Integer.parseInt(cmd[2]);
                    boolean forward = true;
                    if (cmd[3] == "1") forward=false;
                    boolean licht = false;
                    if (cmd[4] == "1") forward=true;
                    boolean horn = false;
                    if (cmd[5] == "1") forward=true;                    
                    sxi.sendLoco(adr, speed, licht, forward, horn);
                    } catch (Exception e) {
                        System.out.println("could not under LOCO command format: "+msg+" error="+e.getMessage());
                    }
                    
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1aa, javax.swing.GroupLayout.DEFAULT_SIZE, 333, Short.MAX_VALUE)
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
                .addComponent(jScrollPane1aa, javax.swing.GroupLayout.DEFAULT_SIZE, 554, Short.MAX_VALUE)
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
