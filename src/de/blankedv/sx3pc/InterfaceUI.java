package de.blankedv.sx3pc;

//import java.io.InputStream;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

/**
 * This class is the SX3 MAIN class and starts all other UI-windows.
 *
 * @author mblank
 *
 */
public class InterfaceUI extends javax.swing.JFrame {

    static final String VERSION = "1.3 - 10 Nov 2016";   // program version, displayed in HELP window
    static final int SXMAX = 112;  // maximal angezeigt im Monitor
    static final int SXMAX_USED = 104;  // maximale Adresse für normale Benutzung (Loco, Weiche, Signal)
    static final int SXMAX2 = 127; // maximal möglich (pro SX Kanal)
    static boolean DEBUG = false;
    static boolean doUpdateFlag = false;
    static boolean running = true;
    static InterfaceUI sx;
    static SXInterface sxi;
    static SettingsUI settingsWindow;
    static int[][] sxData = new int[SXMAX2 + 1][2];   // the [0]=SX0, [1]=SX1
    static boolean twoBusses = false;
    // locos always control on SX0, "schalten/melden" on SX0 or SX1
    static int sxbusControl = 0;
    static MonitorUI sxmon = null;
    static MonitorUI sxmon1 = null;  // for SX1
    static SRCPServerUI srcpserver;
    static SXnetServerUI sxnetserver;
    static LanbahnUI lanbahnserver;
    static ResourceBundle bundle;
    static boolean pollingIsRunning = false;
    static LocoProgUI locoprog = null;
    static VtestUI vtest = null;
    static int timeoutCounter = 0;
    static final int TIMEOUT_SECONDS = 10;  // check for connection every 30secs
    static boolean connectionOK = false;  // watchdog for connection
    OutputStream outputStream;
    InputStream inputStream;
    private Boolean sxiConnected = false;
    ThrottleUI loco1;
    SensorUI sensor1;
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    private String portName;
    private int baudrate;
    private boolean simulation;
    private String ifType;
    Boolean pollingFlag = false;  // only needed for trix-standard IF
    private boolean enableSrcp;
    private boolean enableSxnet;
    private boolean enableLanbahn;
    private Locale loc;
    private ImageIcon green, red;
    private List<Integer> pList = new LinkedList<Integer>();
    Timer timer;  // user for updating UI every second

    /**
     * Creates new form InterfaceUI
     */
    public InterfaceUI() {

        loadWindowPrefs();

        initComponents();
        i18n();
        URL url;
        try {
            url = ClassLoader.getSystemResource("de/blankedv/sx3pc/icons/sx3_ico.png");
            Toolkit kit = Toolkit.getDefaultToolkit();
            Image img = kit.createImage(url);
            setIconImage(img);
        } catch (Exception ex) {
            Logger.getLogger(InterfaceUI.class.getName()).log(Level.SEVERE, null, ex);
        }

        loadOtherPrefs();

        sxi = new SXInterface(!pollingFlag, portName, baudrate, simulation);
        // init status icon
        green = new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/greendot.png"));
        red = new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/reddot.png"));
        statusIcon.setIcon(red);


        // set status text
        if (simulation) {
            labelStatus.setText(bundle.getString("Simul"));
            btnConnectDisconnect.setEnabled(false);
            btnConnectDisconnect.setText(" ");
            btnPowerOnOff.setEnabled(true);  // works always in simulation
            statusIcon.setEnabled(true);  // always works in simulation
        } else {
            labelStatus.setText("SX-Interface " + ifType + " - Port " + portName);
            btnPowerOnOff.setEnabled(false);  // works only after connection
            statusIcon.setEnabled(false);  // works only after connection
        }

        btnSxMonitor.setEnabled(!pollingFlag); // disable for standard trix interface
        addToPlist((Integer) 127);  // Power Status wird immer abgefragt.
        setVisible(true);
        if (enableSxnet) {
           sxnetserver = new SXnetServerUI();
        }
        if (enableSrcp) {
        //    srcpserver = new SRCPServerUI();
        }
        if (enableLanbahn) {
           lanbahnserver = new LanbahnUI();
        }
        
        initTimer();
    }

    private void closeAll() {
        System.out.println("close all.");
        running = false;  // flag for stopping services
        try {  // close jmdns etc.
            Thread.sleep(500);
        } catch (InterruptedException e1) {
            ;
        }
        savePrefs();
        saveAllPrefs();
        sxi.close();
        System.exit(0);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPopupMenu1 = new javax.swing.JPopupMenu();
        jLabel2 = new javax.swing.JLabel();
        panelWindows = new javax.swing.JPanel();
        btnThrottle = new javax.swing.JButton();
        btnTurnout = new javax.swing.JButton();
        btnSensor = new javax.swing.JButton();
        btnSxMonitor = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        btnVtest = new javax.swing.JButton();
        panelInterface = new javax.swing.JPanel();
        btnConnectDisconnect = new javax.swing.JButton();
        btnPowerOnOff = new javax.swing.JButton();
        labelStatus = new javax.swing.JLabel();
        statusIcon = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        menuExit = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        menuSettings = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SX3-PC");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        panelWindows.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Fenster"));

        btnThrottle.setText("+ Lokregler");
        btnThrottle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnThrottleActionPerformed(evt);
            }
        });

        btnTurnout.setText("+ Weichen");
        btnTurnout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTurnoutActionPerformed(evt);
            }
        });

        btnSensor.setText("+ Belegtmelder");
        btnSensor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSensorActionPerformed(evt);
            }
        });

        btnSxMonitor.setText("SX Monitor");
        btnSxMonitor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSxMonitorActionPerformed(evt);
            }
        });

        btnReset.setText("RESET");
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetActionPerformed(evt);
            }
        });

        btnVtest.setText("V-Kennlinie");
        btnVtest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVtestActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelWindowsLayout = new javax.swing.GroupLayout(panelWindows);
        panelWindows.setLayout(panelWindowsLayout);
        panelWindowsLayout.setHorizontalGroup(
            panelWindowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelWindowsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelWindowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnThrottle, javax.swing.GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE)
                    .addComponent(btnVtest, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(35, 35, 35)
                .addGroup(panelWindowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelWindowsLayout.createSequentialGroup()
                        .addComponent(btnTurnout, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
                        .addComponent(btnSensor, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(panelWindowsLayout.createSequentialGroup()
                        .addComponent(btnSxMonitor, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnReset, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        panelWindowsLayout.setVerticalGroup(
            panelWindowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelWindowsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelWindowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnThrottle)
                    .addComponent(btnTurnout)
                    .addComponent(btnSensor))
                .addGap(18, 18, 18)
                .addGroup(panelWindowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnReset)
                    .addComponent(btnSxMonitor)
                    .addComponent(btnVtest))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelInterface.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Interface"));

        btnConnectDisconnect.setText("Verbinden");
        btnConnectDisconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConnectDisconnectActionPerformed(evt);
            }
        });

        btnPowerOnOff.setText("Gleissp. einschalten");
        btnPowerOnOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPowerOnOffActionPerformed(evt);
            }
        });

        labelStatus.setText("??");

        javax.swing.GroupLayout panelInterfaceLayout = new javax.swing.GroupLayout(panelInterface);
        panelInterface.setLayout(panelInterfaceLayout);
        panelInterfaceLayout.setHorizontalGroup(
            panelInterfaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelInterfaceLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelInterfaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelInterfaceLayout.createSequentialGroup()
                        .addComponent(btnConnectDisconnect, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(btnPowerOnOff, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(statusIcon))
                    .addComponent(labelStatus))
                .addContainerGap())
        );
        panelInterfaceLayout.setVerticalGroup(
            panelInterfaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelInterfaceLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(labelStatus)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelInterfaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(statusIcon)
                    .addGroup(panelInterfaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnConnectDisconnect)
                        .addComponent(btnPowerOnOff)))
                .addContainerGap())
        );

        jMenu1.setText("File");

        menuExit.setText("Exit");
        menuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExitActionPerformed(evt);
            }
        });
        jMenu1.add(menuExit);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");

        menuSettings.setText("Settings");
        menuSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSettingsActionPerformed(evt);
            }
        });
        jMenu2.add(menuSettings);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("Help");
        jMenu3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenu3ActionPerformed(evt);
            }
        });

        jMenuItem2.setText("About");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem2);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(panelInterface, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(panelWindows, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelInterface, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panelWindows, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnConnectDisconnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConnectDisconnectActionPerformed
        // this button can never be pressed in simulation mode.
        if (sxiConnected) {
            closeConnection();
        } else {
            if (sxi.open()) {

                statusIcon.setEnabled(true);
                btnConnectDisconnect.setText(bundle.getString("Disconnect"));
                sxiConnected = true;
                btnPowerOnOff.setEnabled(true);
                btnReset.setEnabled(true);
                connectionOK = true;
                timeoutCounter = 0;
            } else {
                JOptionPane.showMessageDialog(this, bundle.getString("CheckSerialSettings"));
            }
        }
    }//GEN-LAST:event_btnConnectDisconnectActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        // TODO add your handling code here:
    }//GEN-LAST:event_formWindowClosed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        System.out.println("formWindowClosing.");
        closeAll();

    }//GEN-LAST:event_formWindowClosing

    private void btnPowerOnOffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPowerOnOffActionPerformed

        if (!sxiConnected && !simulation) {
            JOptionPane.showMessageDialog(this, bundle.getString("PlsConnectFirst"));
            return;
        }
        if (powerIsOn()) {
            sxi.powerOff();
        } else {
            sxi.powerOn();
        }
    }//GEN-LAST:event_btnPowerOnOffActionPerformed

    private void btnThrottleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnThrottleActionPerformed
        loco1 = new ThrottleUI();
    }//GEN-LAST:event_btnThrottleActionPerformed

    private void btnSxMonitorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSxMonitorActionPerformed
        if (sxmon == null) {
            if (twoBusses) {
                sxmon = new MonitorUI(0);
                sxmon1 = new MonitorUI(1);
            } else {
                sxmon = new MonitorUI();
                sxmon1 = null;
            }
            sxmon.update();
            if (twoBusses) {
                sxmon1.update();
            }

        } else {
            JOptionPane.showMessageDialog(this, bundle.getString("SXmonitorRunning"));
        }
    }//GEN-LAST:event_btnSxMonitorActionPerformed

    private void btnSensorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSensorActionPerformed

        new SensorUI();
    }//GEN-LAST:event_btnSensorActionPerformed

    private void btnTurnoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTurnoutActionPerformed
        new WeichenUI();  // TODO select bus
    }//GEN-LAST:event_btnTurnoutActionPerformed

    private void menuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExitActionPerformed
        System.out.println("exit button pressed.");
        closeAll();
    }//GEN-LAST:event_menuExitActionPerformed

    private void menuSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSettingsActionPerformed
        if (settingsWindow == null) {
            settingsWindow = new SettingsUI();
        } else {
            JOptionPane.showMessageDialog(this, bundle.getString("SettingsWindowOpen"));
        }
    }//GEN-LAST:event_menuSettingsActionPerformed

    private void jMenu3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu3ActionPerformed
        ;
    }//GEN-LAST:event_jMenu3ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        new HelpWindowUI();
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetActionPerformed
        if (sxiConnected) {
            Cursor c = this.getCursor();
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            sxi.resetAll();
            this.setCursor(c);
        }
        if (simulation) {
            for (int i = 0; i < SXMAX; i++) {  // nur bis 112, die oberen (=system) Channels werden nicht auf 0 gesetzt
                sxData[i][0] = 0;
                sxData[i][1] = 0;
            }
        }
    }//GEN-LAST:event_btnResetActionPerformed

    private void btnVtestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVtestActionPerformed
        if (vtest == null) {
            vtest = new VtestUI();

        } else {
            JOptionPane.showMessageDialog(this, "already running"); //bundle.getString("SXmonitorRunning"));
        }
    }//GEN-LAST:event_btnVtestActionPerformed

    /**
     * @param argsstatic the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                sx = new InterfaceUI();
                sx.setVisible(true);
            }
        });
    }

    private void initTimer() {
        timer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doUpdate();
                checkConnection();
            }
        });
        timer.start();
    }

    public void addToPlist(Integer a) {  // a vom Typ Integer, nicht int !!!
        pList.add(a);
        if (DEBUG) {
            System.out.println("List " + pList.toString());
        }
    }

    public void removeFromPlist(Integer a) {
        pList.remove(a);  // wichtig: a muss ein Integer-OBJECT sein, kein Integer, sonst wird
        //  nicht das "Object a", sondern das Object an der Position "a" gelöscht.
        if (DEBUG) {
            System.out.println("List " + pList.toString());
        }
    }

    public List<Integer> getpList() {
        return pList;
    }

    public static int toUnsignedInt(byte value) {
        return (value & 0x7F) + (value < 0 ? 128 : 0);
    }

    private void updatePowerBtnAndIcon() {
        // other sources can switch power off and on, therefor
        // regular update needed 

        if (powerIsOn()) {
            btnPowerOnOff.setText(bundle.getString("TurnPowerOff"));
            statusIcon.setIcon(green);
        } else {
            btnPowerOnOff.setText(bundle.getString("TurnPowerOn"));
            statusIcon.setIcon(red);
        }

    }

    public boolean powerIsOn() {
        return ((sxData[127][0] & 0x80) != 0);
    }

    public void doUpdate() {
        //System.out.println("do update called.");
        updatePowerBtnAndIcon();
        if (sxmon != null) {
            sxmon.update();
        }
        if (sxmon1 != null) {
            sxmon1.update();
        }
        SensorUI.updateAll();
        WeichenUI.updateAll();
        ThrottleUI.updateAll();
    }

    private void checkConnection() {
        timeoutCounter++;

        if ((timeoutCounter > TIMEOUT_SECONDS) && (sxiConnected)) {
            sxi.readPower();
            try {
                Thread.sleep(50);
            } catch (Exception e) {;
            };  // wait a few milliseconds for response
            // check if connectionOK flag was reset
            if (!connectionOK) {
                JOptionPane.showMessageDialog(this, "Verbindung verloren !! ");
                closeConnection();
            } else {
                connectionOK = false; // will be set to true in receive routine
            }
            timeoutCounter = 0; // reset counter
        }
    }

    private void closeConnection() {
        if (sxiConnected) {
            sxi.close();
            sxiConnected = false;
        }
        statusIcon.setEnabled(false);
        btnConnectDisconnect.setText(bundle.getString("Connect"));
        btnPowerOnOff.setEnabled(false);
        btnReset.setEnabled(false);
        connectionOK = false;
    }

    public void saveAllPrefs() {
        //System.out.println("save all preferences.");

        if (sxmon != null) {
            sxmon.savePrefs();
        }
        if (sxmon1 != null) {
            sxmon1.savePrefs();
        }
        SensorUI.saveAllPrefs();
        WeichenUI.saveAllPrefs();
        ThrottleUI.saveAllPrefs();
        if (srcpserver != null) {
            srcpserver.savePrefs();
        }
        if (sxnetserver != null) {
            sxnetserver.savePrefs();
        }
         if (lanbahnserver != null) {
            lanbahnserver.savePrefs();
        }
    }

    private void savePrefs() {
        // Fensterpositionen speichern
        prefs.putInt("windowX", getX());
        prefs.putInt("windowY", getY());
    }

    private void loadWindowPrefs() {
        setLocation(prefs.getInt("windowX", 200), prefs.getInt("windowY", 200));
        DEBUG = prefs.getBoolean("enableDebug", false);
        System.out.println("DEBUG=" + DEBUG);
        String l = prefs.get("locale", "Deutsch");
        if (l.contains("Deu")) {
            loc = Locale.GERMAN;
        } else {
            loc = Locale.ENGLISH;
        }
        bundle = ResourceBundle.getBundle("resources", loc);
        if (DEBUG) {
            System.out.println("lang=" + loc.toString());
        }
    }

    private void loadOtherPrefs() {
        portName = prefs.get("commPort", "/dev/ttyS0");
        simulation = prefs.getBoolean("simulation", false);
        enableSrcp = prefs.getBoolean("enableSRCP", false);
        enableSxnet = prefs.getBoolean("enableSxnet", false);
        enableLanbahn = prefs.getBoolean("enableLanbahn", false);

        ifType = prefs.get("type", "");
        if (ifType.toLowerCase().contains("66824") && (!simulation)) {
            pollingFlag = true;
        } else {
            pollingFlag = false; // wird auch bei Simulation verwendet.
        }

        String busmode = prefs.get("busmode", "SX");
        if (busmode.equalsIgnoreCase("SX0/SX1")) {
            twoBusses = true;
            sxbusControl = prefs.getInt("sxbusControl", 1);
            if (DEBUG) { System.out.println("2 SX buses, control=SX"+sxbusControl);}
        } else {
            twoBusses = false;
            sxbusControl = 0;
            if (DEBUG) { System.out.println("1 SX bus ");}
        }
        String baudStr = prefs.get("baudrate", "9600");
        baudrate = Integer.parseInt(baudStr);
        if (DEBUG) { System.out.println("IF="+ifType+" serial port="+portName+" at "+baudrate+" baud");}

        // all sensors need polling (for srcp and/or Standard interface)
        String sel = prefs.get("SensorList", "");
        if (DEBUG) {
            System.out.println("reading sensors:" + sel);
        }
        String[] slist = sel.split(";");
        if (slist.length > 0 && !slist[0].isEmpty()) {
            for (int i = 0; i < slist.length; i++) {
                addToPlist(Integer.parseInt(slist[i]));
            }
        }
    }

    private void i18n() {
        // language init for language dependent UI labels
        btnThrottle.setText(bundle.getString("Throttle"));
        btnTurnout.setText(bundle.getString("TurnoutEtc"));
        btnSensor.setText(bundle.getString("Sensor"));
        TitledBorder tb1 = (TitledBorder) panelWindows.getBorder();
        tb1.setTitle(bundle.getString("Windows"));
        tb1 = (TitledBorder) panelInterface.getBorder();
        tb1.setTitle(bundle.getString("Interface"));

    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnConnectDisconnect;
    private javax.swing.JButton btnPowerOnOff;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnSensor;
    private javax.swing.JButton btnSxMonitor;
    private javax.swing.JButton btnThrottle;
    private javax.swing.JButton btnTurnout;
    private javax.swing.JButton btnVtest;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JLabel labelStatus;
    private javax.swing.JMenuItem menuExit;
    private javax.swing.JMenuItem menuSettings;
    private javax.swing.JPanel panelInterface;
    private javax.swing.JPanel panelWindows;
    private javax.swing.JLabel statusIcon;
    // End of variables declaration//GEN-END:variables
}
