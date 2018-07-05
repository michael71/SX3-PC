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
 * @author mblank
 *
 */
public class MainUI extends javax.swing.JFrame {

    public static final String VERSION = "1.67 - 04 Jul 2018";   // program version, displayed in HELP window 
    public static final int SXMAX = 112;  // maximal angezeigt im Monitor
    public static final int SXMAX_USED = 104;  // maximale Adresse für normale Benutzung (Loco, Weiche, Signal)
    public static final int SXMAX2 = 128; // maximal möglich (pro SX Kanal)
    public static final int N_LANBAHN = 500;  // number of entries in lanbahn array 
                                 //(i.e. maximum number of usable lanbahn addresses
    public static final int LBMAX = 9999;  // maximum lanbahn channel number
    public static final int INVALID_INT = -1;
    public static boolean DEBUG = true;
    public static final boolean doUpdateFlag = false;
    public static boolean running = true;
    public static boolean simulation;
    public static final int CONFIG_PORT = 8000;
    public static MainUI sx;
    public static GenericSXInterface sxi;
    public static SettingsUI settingsWindow;
    public static final int NBUSSES = 2;   // 0 => SX0, 1 => SX1 (if it exists)
    // locos: always SX0   
    // control(turnouts, signals, buttons, routes) => SX0 OR SX1   
    
    public static final int[][] sxData = new int[SXMAX2][NBUSSES];   // the [0]=SX0, [1]=SX1
    public static final ArrayList<LanbahnSXPair> allLanbahnSXPairs = new ArrayList<>();  // maps lanbahn addresses to SX addresses
    public static final ArrayList<LocoNetSXPair> allLocoNetSXPairs = new ArrayList<>();  // maps loconet (DCC) addresses to SX addresses
    
    // lanbahnData = hashmap for storing numerical (key,value) pairs of lanbahnData
    // lanbahn loco data (strings) are always converted to SX0 values
    public static final ConcurrentHashMap<Integer,Integer> lanbahnData = new ConcurrentHashMap<Integer,Integer>(N_LANBAHN);

    public static boolean useSX1forControl = false;
    public static int sxbusControl = 0;
    public static MonitorUI sxmon[] = {null, null};
    public static LanbahnMonitorUI lbmon = null;
    public static SXnetServerUI sxnetserver;
    public static List<InetAddress> myip;
    public static LanbahnUI lanbahnserver;
    public static boolean pollingIsRunning = false;
    public static LocoProgUI locoprog = null;
    public static VtestUI vtest = null;
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

    Boolean pollingFlag = false;  // only needed for trix-standard IF

    private boolean enableSxnet;
    private boolean enableLanbahn;
    
    private ConfigWebserver configWebserver;
    

    private final ImageIcon green, red;
    private List<Integer> pList = new LinkedList<>();
    Timer timer;  // user for updating UI every second

    /**
     * Creates new form InterfaceUI
     */
    public MainUI() throws Exception {

        // get network info
        myip = NIC.getmyip();   // only the first one will be used
        System.out.println("Number of usable Network Interfaces=" + myip.size());

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

        } else if ( ifType.contains("FCC") ) { // fcc has different interface handling ! 
            sxi = new SXFCCInterface(portName);
        } else if ( ifType.toLowerCase().contains("opensx") ) { // opensx has different interface handling !
             sxi = new SXOpenSXInterface(portName);
        } else {
            //portName = "/dev/ttyUSB825";
            sxi = new SXInterface(!pollingFlag, portName, baudrate);
        }
        
        // init status icon
        green = new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/greendot.png"));
        red = new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/reddot.png"));
        statusIcon.setIcon(red);

        // set status text
        if (simulation) {
            if ( ifType.contains("ZS1") || ifType.contains("FCC") || ifType.contains("SLX 852")) {
                labelStatus.setText("Simulation SX0/SX1");
            } else {
                labelStatus.setText("Simulation SX0");
            }
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

        btnSxMonitor.setEnabled(!pollingFlag); // disable for standard trix interface
        addToPlist((Integer) 127);  // Power Status wird immer abgefragt.
        setVisible(true);

        // get network info
        myip = NIC.getmyip();   // only the first one will be used
        System.out.println("Number of usable Network Interfaces=" + myip.size());

        String configFile = prefs.get("configfilename", "-keiner-");
        
        if (myip.size() >= 1) {  // makes only sense when we have network connectivity
            if (enableSxnet) {
                sxnetserver = new SXnetServerUI();
            }

            if (enableLanbahn) {
                lanbahnserver = new LanbahnUI();
            }
            
           // configWebserver = new ConfigWebserver(configFile,CONFIG_PORT);
        }

        initTimer();
        
        UtilityMapping.init(configFile);
        this.setTitle("SX3-PC - "+panelName);
        
    }

    private void closeAll() {
        System.out.println("close all.");
        running = false;  // flag for stopping services
        
        if (configWebserver != null) {
            // stop webserver
            configWebserver.stop();
        }
        
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
    
    private boolean powerIsOn() {
        if (sxData[127][0] != 0) {
            return true;
        } else {
            return false;
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

        btnVtest.setText("Speed Meas.");
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
        if (sxmon[0] == null) {
            if (useSX1forControl) {
                sxmon[1] = new MonitorUI(1);
                sxmon[1].update();
            } else {
                sxmon[1] = null;
            }

            sxmon[0] = new MonitorUI(0);
            sxmon[0].update();

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
            JOptionPane.showMessageDialog(this, "Settings Window Open");
        }
    }//GEN-LAST:event_menuSettingsActionPerformed

    private void jMenu3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu3ActionPerformed
        ;
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
                sxData[i][0] = 0;
                sxData[i][1] = 0;
            }
        }
    }//GEN-LAST:event_btnResetActionPerformed

    private void btnVtestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVtestActionPerformed
        if (vtest == null) {
            vtest = new VtestUI();

        } else {
            JOptionPane.showMessageDialog(this, "already running");
        }
    }//GEN-LAST:event_btnVtestActionPerformed

    
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
     * @param argsstatic the command line arguments
     */
    public static void main(String args[]) {

        try {
            // Set cross-platform Java L&F (also called "Metal")
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException e) {
            // handle exception
        } catch (ClassNotFoundException e) {
            // handle exception
        } catch (InstantiationException e) {
            // handle exception
        } catch (IllegalAccessException e) {
            // handle exception
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    sx = new MainUI();
                } catch (Exception ex) {
                    Logger.getLogger(MainUI.class.getName()).log(Level.SEVERE, null, ex);
                }
                sx.setVisible(true);
            }
        });
    }

    /** 250 msec update timer for FCC
     *  1000 msecs used for GUI update
     */
    private void initTimer() {
        timer = new Timer(250, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doUpdate();

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

        if (sxData[127][0] == 0x00) {
            btnPowerOnOff.setText("Track Power On");
            statusIcon.setIcon(red);
        } else {
            btnPowerOnOff.setText("Track Power Off");
            statusIcon.setIcon(green);
        }

    }

    
    public void doUpdate() {
        String result = sxi.doUpdate();
        if (!result.isEmpty()) {
            JOptionPane.showMessageDialog(this, result);
            toggleConnectStatus();
        }
        updateCount++;
        if (updateCount < 4) return;
        
        updateCount = 0;
        checkConnection();
        
        // do GUI update only every second
        //System.out.println("do update called.");
        updatePowerBtnAndIcon();
        for (int i = 0; i < NBUSSES; i++) {
            if (sxmon[i] != null) {
                sxmon[i].update();
            }
        }
        if (lbmon != null) lbmon.update();

        SensorUI.updateAll();
        WeichenUI.updateAll();
        ThrottleUI.updateAll();
        FunkreglerUI.updateAll();
        FunkreglerUI.checkAlive();

    }

    /** called every second
     * 
     */
    private void checkConnection() {
        if (simulation) return;
        
        timeoutCounter++;

        if ((timeoutCounter > TIMEOUT_SECONDS) && (sxi.isConnected())) {
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

        for (int i = 0; i < NBUSSES; i++) {
            if (sxmon[i] != null) {
                sxmon[i].savePrefs();
            }
        }

        SensorUI.saveAllPrefs();
        WeichenUI.saveAllPrefs();
        ThrottleUI.saveAllPrefs();
        FunkreglerUI.saveAllPrefs();

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

    }

    private void loadOtherPrefs() {
        portName = prefs.get("commPort", "/dev/ttyUSS0");
        simulation = prefs.getBoolean("simulation", false);
        System.out.println("simulation=" +simulation);
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
            useSX1forControl = true;
            sxbusControl = prefs.getInt("sxbusControl", 1);
            if (DEBUG) {
                System.out.println("2 SX buses, control=SX" + sxbusControl);
            }
        } else {
            useSX1forControl = false;
            sxbusControl = 0;
            if (DEBUG) {
                System.out.println("1 SX bus ");
            }
        }
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
