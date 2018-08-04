package de.blankedv.sx3pc;

//import java.io.InputStream;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * This class is the SX3 MAIN class and starts all other UI-windows.
 *
 * now (July 2018) using protocol version 3: separation of SX and Lanbahn
 * commands S 47 67 == SX Command, Feedback returned X 47 67 Read channel : R 47
 * SET 902 1 == LANBAHN COMMAND (internally interpretet as "set addr 90, bit 2
 * FEEDBACK RETURN XS 902 1 READ 987 == READ LANBAHN CHANNEL 987
 *
 * @author mblank
 *
 */
public class MainUI extends javax.swing.JFrame {

    /**
     * {@value #VERSION} = program version, displayed in HELP window
     */
    public static final String VERSION = "2.30 - 04 Aug 2018";
    public static final String S_XNET_SERVER_REV = "SXnet-Server 3.1 - 04 Aug 2018";

    /**
     * {@value #SX_MIN} = minimale SX adresse angezeigt im Monitor
     */
    public static final int SXMIN = 0;
    /**
     * maximale SX adresse (SX0), maximale adr angezeigt im Monitor
     */
    public static final int SXMAX = 111;
    /**
     * {@value #SX_MAX_USED} = maximale Adresse für normale Benutzung (Loco,
     * Weiche, Signal) higher addresses reserved for command stations/loco
     * programming
     */
    public static final int SXMAX_USED = 104;
    /**
     * {@value #SX_POWER} = virtual addr to transmit power state
     */
    public static final int SXPOWER = 127;   // 
    /**
     * {@value #N_SX} maximum index for data arrays
     */
    public static final int N_SX = 128; // maximal möglich (pro SX Kanal)

    /**
     * {@value #N_LANBAHN} number of entries in lanbahn array (i.e. maximum
     * number of usable lanbahn addresses)
     */
    public static final int N_LANBAHN = 500;
    /**
     * {@value #LBMIN} minimum lanbahn channel number
     */
    public static final int LBMIN = 1; // 

    /**
     * {@value #LBMAX} =maximum lanbahn channel number
     */
    public static final int LBMAX = 9999;
    /**
     * {@value #LBDATAMIN} =minimum lanbahn data value
     */
    public static final int LBDATAMIN = 0;
    /**
     * {@value #LBDATAMAX} =maximum lanbahn data value (== 4 bits in SX world)
     */
    public static final int LBDATAMAX = 15;  // 
    /**
     * {@value #INVALID_INT} = denotes a value as invalid (not usable)
     */
    public static final int INVALID_INT = -1;
    public static final int STATUS_CONNECTED = 1;
    public static final int STATUS_NOT_CONNECTED = 0;
    public static boolean DEBUG = true;
    public static final boolean doUpdateFlag = false;
    public static boolean running = true;
    public static boolean simulation;
    /**
     * {@value #CONFIG_PORT} = use this port to request config.xml from
     * webserver url = "http://hostname:{@value #CONFIG_PORT}/config"
     */
    public static final int CONFIG_PORT = 8000;
    public static MainUI sx;
    public static GenericSXInterface sxi;
    public static SettingsUI settingsWindow;

    /**
     * contains the complete state of command station
     */
    public static final int[] sxData = new int[N_SX];
    /**
     * locoAddresses ArrayList contains addresses of all locos to be able to
     * generate loco specific feedback messages
     */
    public static final ArrayList<Integer> locoAddresses = new ArrayList<Integer>();
    public static final ConcurrentHashMap<Integer, Integer> lanbahnData = new ConcurrentHashMap<Integer, Integer>(N_LANBAHN);
    public static final ArrayList<SignalMapping> allSignalMappings = new ArrayList<SignalMapping>();
    public static MonitorUI sxmon = null;
    public static LanbahnMonitorUI lbmon = null;

    public static SXnetServerUI sxnetserver;
    public static List<InetAddress> myip;
    public static int timeoutCounter = 0;
    public static final int TIMEOUT_SECONDS = 10;  // check for connection every 30secs
    public static boolean connectionOK = false;  // watchdog for connection
    public static String panelName = "";
    public static String panelControl = "";  // command station type

    OutputStream outputStream;
    InputStream inputStream;

    private static int updateCount = 0;  //for counting 250 msec timer
    ThrottleUI loco1;
    SensorUI sensor1;
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    private String portName;
    private int baudrate;

    private String ifType;

    private ConfigWebserver configWebserver;

    private final ImageIcon green, red;
    private List<Integer> pList = new LinkedList<>();
    Timer timer;  // user for updating UI every second
    private String downloadFrom;

    /**
     * Creates new form InterfaceUI
     */
    public MainUI() throws Exception {

        // get network info
        myip = NIC.getmyip();   // only the first one will be used
        System.out.println("Number of usable Network Interfaces=" + myip.size());
        if (myip.size() == 0) {
            System.out.println("ERROR: not network !!! cannot do anything");
            downloadFrom = "download von http://hostname:8000/config  .../loco";
        } else {
            downloadFrom = "download from http:/" + myip.get(0).toString() + ":8000/config   .../loco";
        }

        loadWindowPrefs();

        initComponents();

        URL url;
        try {
            url = ClassLoader.getSystemResource("de/blankedv/sx3pc/icons/sx3_ico.png");
            Toolkit kit = Toolkit.getDefaultToolkit();
            Image img = kit.createImage(url);
            setIconImage(img);
        } catch (Exception ex) {
            Logger.getLogger(MainUI.class.getName()).log(Level.SEVERE, null, ex);
        }

        loadOtherPrefs();
        if (simulation) {
            sxi = new SXSimulationInterface();
        } else if (ifType.contains("FCC")) { // fcc has different interface handling ! 
            sxi = new SXFCCInterface(portName);
        } else {
            //portName = "/dev/ttyUSB825";
            sxi = new SXInterface(portName, baudrate);
        }

        // init status icon
        green = new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/greendot.png"));
        red = new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/reddot.png"));
        statusIcon.setIcon(red);

        // set status text
        if (simulation) {
            labelStatus.setText("Simulation SX0");
            sxi.open();
            btnConnectDisconnect.setEnabled(false);
            btnConnectDisconnect.setText(" ");
            btnPowerOnOff.setEnabled(true);  // works always in simulation
            statusIcon.setEnabled(true);  // always works in simulation
        } else {
            labelStatus.setText("SX-Interface " + ifType + " - Port " + portName);
            btnPowerOnOff.setEnabled(false);  // works only after connection
            statusIcon.setEnabled(false);  // works only after connection
        }

        btnSxMonitor.setEnabled(true); // disable for standard trix interface
        addToPlist((Integer) 127);  // Power Status wird immer abgefragt.
        setVisible(true);

        // get network info
        myip = NIC.getmyip();   // only the first one will be used
        System.out.println("Number of usable Network Interfaces=" + myip.size());

        String configFile = prefs.get("configfilename", "-keiner-");
        String locoConfigFile = prefs.get("locofilename", "");

        initTimer();

        this.setTitle("SX3-PC"); // + panelName);

        if (myip.size() >= 1) {  // makes only sense when we have network connectivity
            sxnetserver = new SXnetServerUI();
            sxnetserver.setVisible(true);

            if (!configFile.equalsIgnoreCase("-keiner-")) {
                configWebserver = new ConfigWebserver(configFile, locoConfigFile, CONFIG_PORT);
                lblMainConfigFilename.setText(configFile);
                lblMainLocoConfigFilename.setText(locoConfigFile);
                ReadSignalMapping.init(configFile);

            } else {
                lblMainConfigFilename.setText("bisher nicht ausgewählt");
                lblMainLocoConfigFilename.setText(locoConfigFile);
            }

        } else {
            lblMainConfigFilename.setText("kein Netzwerk!!");
            lblMainLocoConfigFilename.setText("");
            JOptionPane.showMessageDialog(this, "ERROR no network, cannot start SXnet");

        }

    }

    private void closeAll() {
        System.out.println("close all.");
        running = false;  // flag for stopping services

        if (configWebserver != null) {
            // stop webserver
            configWebserver.stop();
        }

        try {  // close threads
            Thread.sleep(500);
        } catch (InterruptedException e1) {
            ;
        }
        savePrefs();
        saveAllPrefs();
        sxi.close();
        System.exit(0);
    }

    private boolean powerIsOn() {
        return (sxData[127] & 0x80) != 0; // bit8 of channel 127
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
        panelInterface = new javax.swing.JPanel();
        btnConnectDisconnect = new javax.swing.JButton();
        btnPowerOnOff = new javax.swing.JButton();
        labelStatus = new javax.swing.JLabel();
        statusIcon = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        lblMainConfigFilename = new javax.swing.JLabel();
        lblMainLocoConfigFilename = new javax.swing.JLabel();
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

        btnThrottle.setText("+Throttle");
        btnThrottle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnThrottleActionPerformed(evt);
            }
        });

        btnTurnout.setText("+Turnouts");
        btnTurnout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTurnoutActionPerformed(evt);
            }
        });

        btnSensor.setText("+Sensors");
        btnSensor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSensorActionPerformed(evt);
            }
        });

        btnSxMonitor.setText("Monitor");
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

        javax.swing.GroupLayout panelWindowsLayout = new javax.swing.GroupLayout(panelWindows);
        panelWindows.setLayout(panelWindowsLayout);
        panelWindowsLayout.setHorizontalGroup(
            panelWindowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelWindowsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnThrottle, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                    .addComponent(btnSxMonitor))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelInterface.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Interface"));

        btnConnectDisconnect.setText("Connect");
        btnConnectDisconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConnectDisconnectActionPerformed(evt);
            }
        });

        btnPowerOnOff.setText("Track Power ON");
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

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), downloadFrom));

        lblMainConfigFilename.setFont(new java.awt.Font("Ubuntu", 0, 12)); // NOI18N
        lblMainConfigFilename.setText("jLabel1");

        lblMainLocoConfigFilename.setFont(new java.awt.Font("Ubuntu", 0, 12)); // NOI18N
        lblMainLocoConfigFilename.setText("jLabel1");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblMainConfigFilename)
                    .addComponent(lblMainLocoConfigFilename))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(lblMainConfigFilename, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblMainLocoConfigFilename, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(panelInterface, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(panelWindows, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(20, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnConnectDisconnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConnectDisconnectActionPerformed
        // this button can never be pressed in simulation mode.
        toggleConnectStatus();

    }//GEN-LAST:event_btnConnectDisconnectActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        // TODO add your handling code here:
    }//GEN-LAST:event_formWindowClosed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        System.out.println("formWindowClosing.");
        closeAll();

    }//GEN-LAST:event_formWindowClosing

    private void btnPowerOnOffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPowerOnOffActionPerformed

        if (!sxi.isConnected() && !simulation) {
            JOptionPane.showMessageDialog(this, "Please Conncect First");
            return;
        }
        if (powerIsOn()) {
            sxi.switchPowerOff();
        } else {
            sxi.switchPowerOn();
        }
    }//GEN-LAST:event_btnPowerOnOffActionPerformed

    private void btnThrottleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnThrottleActionPerformed
        loco1 = new ThrottleUI();
    }//GEN-LAST:event_btnThrottleActionPerformed

    private void btnSxMonitorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSxMonitorActionPerformed
        if (sxmon == null) {
            sxmon = new MonitorUI();
            sxmon.update();

            lbmon = new LanbahnMonitorUI();
        } else {
            JOptionPane.showMessageDialog(this, "SXmonitorRunning");
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
            JOptionPane.showMessageDialog(this, "Settings window was already openend");
        }
    }//GEN-LAST:event_menuSettingsActionPerformed

    private void jMenu3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu3ActionPerformed
    }//GEN-LAST:event_jMenu3ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        new HelpWindowUI();
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetActionPerformed
        if (sxi.isConnected()) {
            Cursor c = this.getCursor();
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            sxi.resetAll();
            this.setCursor(c);
        }
        if (simulation) {
            for (int i = 0; i < SXMAX_USED; i++) {  // nur bis 103, die oberen (=system) Channels werden nicht auf 0 gesetzt
                sxData[i] = 0;
            }
        }
    }//GEN-LAST:event_btnResetActionPerformed

    private void toggleConnectStatus() {
        if (sxi.isConnected()) {
            closeConnection();
        } else {
            if (sxi.open()) {
                statusIcon.setEnabled(true);
                btnConnectDisconnect.setText("Disconnect");
                btnPowerOnOff.setEnabled(true);
                btnReset.setEnabled(true);
                connectionOK = true;
                timeoutCounter = 0;
            } else {
                JOptionPane.showMessageDialog(this, "Check Serial Port Settings");
             }
        }
    }

    /**
     *
     *
     */
    public static void main(String args[]) {

        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            // handle exception
        }

        java.awt.EventQueue.invokeLater(() -> {
            try {
                sx = new MainUI();
            } catch (Exception ex) {
                Logger.getLogger(MainUI.class.getName()).log(Level.SEVERE, null, ex);
            }
            sx.setVisible(true);
        });
    }

    /**
     * 250 msec update timer for FCC , 1000 msecs used for GUI update
     */
    private void initTimer() {
        timer = new Timer(250, (ActionEvent e) -> {
            doUpdate();
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

        if (sxData[127] == 0x00) {
            btnPowerOnOff.setText("Track Power On");
            statusIcon.setIcon(red);
        } else {
            btnPowerOnOff.setText("Track Power Off");
            statusIcon.setIcon(green);
        }

    }

    /**
     * called every 250 msecs
     *
     */
    public void doUpdate() {
        String result = sxi.doUpdate();
        if (!result.isEmpty()) {
            JOptionPane.showMessageDialog(this, result);
            toggleConnectStatus();
        }
        updateCount++;
        if (updateCount < 4) {  // do GUI update only every second
            return;
        }

        updateCount = 0;
        checkConnection();

        //System.out.println("do update called.");
        updatePowerBtnAndIcon();
        if (sxmon != null) {
            sxmon.update();
        }

        if (lbmon
                != null) {
            lbmon.update();
        }

        SensorUI.updateAll();

        WeichenUI.updateAll();

        ThrottleUI.updateAll();

        FunkreglerUI.updateAll();

        FunkreglerUI.checkAlive();

    }

    /**
     * called every second
     *
     */
    private void checkConnection() {
        if (simulation) {
            return;
        }

        timeoutCounter++;

        if ((timeoutCounter > TIMEOUT_SECONDS) && (sxi.isConnected())) {
            sxi.readPower();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            // wait a few milliseconds for response
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
        if (sxi.isConnected()) {
            sxi.close();

        }
        statusIcon.setEnabled(false);
        btnConnectDisconnect.setText("Connect");
        btnPowerOnOff.setEnabled(false);
        btnReset.setEnabled(false);
        connectionOK = false;
          }

    public void saveAllPrefs() {
        //System.out.println("save all preferences.");

        if (sxmon != null) {
            sxmon.savePrefs();
        }

        SensorUI.saveAllPrefs();
        WeichenUI.saveAllPrefs();
        ThrottleUI.saveAllPrefs();
        FunkreglerUI.saveAllPrefs();

        if (sxnetserver != null) {
            sxnetserver.savePrefs();
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

    }

    private void loadOtherPrefs() {
        portName = prefs.get("commPort", "/dev/ttyUSS0");
        simulation = prefs.getBoolean("simulation", false);
        System.out.println("simulation=" + simulation);

        ifType = prefs.get("type", "");

        String baudStr = prefs.get("baudrate", "9600");
        baudrate = Integer.parseInt(baudStr);
        if (DEBUG) {
            System.out.println("IF=" + ifType + " serial port=" + portName + " at " + baudrate + " baud");
        }

        // all sensors need polling (for srcp and/or Standard interface)
        String sel = prefs.get("SensorList", "");
        if (DEBUG) {
            System.out.println("reading sensors:" + sel);
        }

        // TODO : is redundant for Rautenhaus and FCC interface -> remove ??
        String[] slist = sel.split(";");
        if (slist.length > 0 && !slist[0].isEmpty()) {
            for (int i = 0; i < slist.length; i++) {
                addToPlist(Integer.parseInt(slist[i]));
            }
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnConnectDisconnect;
    private javax.swing.JButton btnPowerOnOff;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnSensor;
    private javax.swing.JButton btnSxMonitor;
    private javax.swing.JButton btnThrottle;
    private javax.swing.JButton btnTurnout;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JLabel labelStatus;
    private javax.swing.JLabel lblMainConfigFilename;
    private javax.swing.JLabel lblMainLocoConfigFilename;
    private javax.swing.JMenuItem menuExit;
    private javax.swing.JMenuItem menuSettings;
    private javax.swing.JPanel panelInterface;
    private javax.swing.JPanel panelWindows;
    private javax.swing.JLabel statusIcon;
    // End of variables declaration//GEN-END:variables
}
