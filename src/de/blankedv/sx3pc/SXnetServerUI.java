package de.blankedv.sx3pc;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;

import static de.blankedv.sx3pc.InterfaceUI.running;

/**
 *
 * @author mblank
 */
public class SXnetServerUI extends javax.swing.JFrame {

    private static final long serialVersionUID = 534251256456436L;
    private static final int SXNET_PORT = 4104;
    static SXnetServer server;
    // Preferences
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    protected Thread t;
    protected RegisterJMDNSService serv;

     private ServerSocket s;
     
    /**
     * Creates new form SRCPServerUI
     */
    public SXnetServerUI() {
        initComponents();
        loadPrefs();
        List<InetAddress> myip = NIC.getmyip();   // only the first one will be used
        if (!myip.isEmpty()) {
            try {
                s = new ServerSocket(SXNET_PORT);
                // s = new ServerSocket(SXNET_PORT,0,myip.get(0));  
                // only listen on 1 address on multi homed systems
                System.out.println("new sxnet server socket "+myip.get(0)+ ":" + SXNET_PORT);
            } catch (IOException ex) {
                System.out.println("could not open server socket on port=" + SXNET_PORT + " - closing SXnet window.");
                JOptionPane.showMessageDialog(null, "could not open SXnet server socket!\n" + ex.toString(), "Error", JOptionPane.OK_CANCEL_OPTION);
                return;
            }
            startSXnetServer();
            setVisible(true);
            new Thread(new RegisterJMDNSService("sxnet", SXNET_PORT, myip.get(0), this)).start();
        }
    }

    public void displayMessage(String s) {
        taClients.append(s + "\"");
    }

    private void startSXnetServer() {
        if (server == null) {
            server = new SXnetServer();
            t = new Thread(server);
            t.start();

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

        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        taClients = new javax.swing.JTextArea();

        jList1.setModel(new javax.swing.AbstractListModel() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;
            String[] strings = {"Item 1", "Item 2", "Item 3", "Item 4", "Item 5"};

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        jScrollPane1.setViewportView(jList1);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("SXnet Server");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jLabel1.setText("accepting SX net clients at port " + SXNET_PORT);

        taClients.setColumns(20);
        taClients.setEditable(false);
        taClients.setRows(3);
        taClients.setAutoscrolls(true);
        jScrollPane2.setViewportView(taClients);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
                .addComponent(jLabel1))
                .addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 241, Short.MAX_VALUE)
                .addContainerGap()));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        savePrefs();
        System.out.println("SXnet server closed.");
        setVisible(false);
   
    }//GEN-LAST:event_formWindowClosing

    /**
     * @param args the command line arguments
     */
    public void savePrefs() {
        // called when main prog is closing
        // fuer SX3 Programm, zB Belegtmelder: Instanz-Nummer (Klassenvariable) mit im
        // Pfad, um mehrere Fensterpositionen zu speichern
        // auch SX-adresse jeweils speichern.
        prefs.putInt("SXnetwindowX", getX());
        prefs.putInt("SXnetwindowY", getY());
    }

    private void loadPrefs() {
        setLocation(prefs.getInt("SXnetwindowX", 170), prefs.getInt("SXnetwindowY", 170));
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JList jList1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    public static javax.swing.JTextArea taClients;
    // End of variables declaration//GEN-END:variables

    class SXnetServer implements Runnable {

       

        public void run() {
            try {              
                while (running) {
                    Socket incoming = s.accept();  // wait for client to connect

                    taClients.append("new client connected " + incoming.getRemoteSocketAddress().toString() + "\n");

                    // after new client has connected start new thread to handle this client
                    Runnable r = new SXnetSession(incoming);
                    Thread t = new Thread(r);
                    t.start();
                }
                System.out.println("SXnet Server closing.");
                s.close();
            } catch (InterruptedIOException e1) {
                try {
                    System.out.println("SXnet Server interrupted, closing socket");
                    s.close();
                } catch (IOException ex) {
                    System.out.println("closing error " + ex);
                }
            } catch (IOException ex) {
                System.out.println("SXnetServer error:" + ex);
            }

        }
    }
    
   
}
