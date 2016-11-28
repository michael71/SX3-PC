/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ThrottleUI.java
 *
 * Created on 05.04.2011, 18:08:49
 */
package de.blankedv.sx3pc;

import java.util.prefs.Preferences;
import java.util.List;
import java.util.ArrayList;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import static de.blankedv.sx3pc.InterfaceUI.*;   // DAS SX interface.

/**
 *
 * @author mblank
 */
public class ThrottleUI extends javax.swing.JFrame implements MouseWheelListener {
    private static final long serialVersionUID = 534251256456410L;
    private boolean forward = true;
    private int speed = 0;
    private boolean licht = false;
    private boolean horn = false;
    private int lok_adr = 1;
    static List<ThrottleUI> tl = new ArrayList<ThrottleUI>();  //Liste, damit alle Fenster die Updates bekommen
    private int myInstance;
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    static int ThrottleUIInstance = 0;

    public static void updateAll() {
        for (ThrottleUI t : tl) {
            t.update();
        }
    }

    public static void saveAllPrefs() {
        for (ThrottleUI t : tl) {
            t.savePrefs();
        }
    }

    /**
     * Creates new form ThrottleUI
     */
    public ThrottleUI() {
        initComponents();
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        final int w = screenSize.width;
        final int h = screenSize.height;
        this.setLocation(w / 4, h / 4);

        myInstance = ThrottleUIInstance++;
        loadPrefs(); //myInstance is used here.
        comboSelAddress.setSelectedIndex(lok_adr - 1);  // index starts from 0, addresses +1


        update(); // from SX Bus data
        this.setVisible(true);

        tl.add(this);  // add to list of Throttles.

        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                int rotation = e.getWheelRotation();
                int amount = e.getScrollAmount();
                //System.out.println("rot" + rotation + " amount" + amount);
                if (rotation < 0) {
                    speedUp();
                    updateLoco();
                } else {
                    speedDown();
                    updateLoco();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {

                if ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0) {
                    toggleDirection();
                }
            }
        });

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblAddress = new javax.swing.JLabel();
        comboSelAddress = new javax.swing.JComboBox<>();
        jSliderSpeed = new javax.swing.JSlider();
        cbLight = new javax.swing.JCheckBox();
        cbHorn = new javax.swing.JCheckBox();
        btnStop = new javax.swing.JButton();
        btnChangeDir = new javax.swing.JButton();
        labelDir = new javax.swing.JLabel();

        setTitle("Lokregler");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        lblAddress.setText("Addr");

        comboSelAddress.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "100", "101", "102", "103", "104", "105", "106", "107", "108", "109", "110", "111" }));
        comboSelAddress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboSelAddressActionPerformed(evt);
            }
        });

        jSliderSpeed.setMajorTickSpacing(5);
        jSliderSpeed.setMaximum(31);
        jSliderSpeed.setOrientation(javax.swing.JSlider.VERTICAL);
        jSliderSpeed.setPaintLabels(true);
        jSliderSpeed.setPaintTicks(true);
        jSliderSpeed.setValue(0);
        jSliderSpeed.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSliderSpeedStateChanged(evt);
            }
        });
        jSliderSpeed.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jSliderSpeedPropertyChange(evt);
            }
        });

        cbLight.setText("Light");
        cbLight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbLightActionPerformed(evt);
            }
        });

        cbHorn.setText("Horn");
        cbHorn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbHornActionPerformed(evt);
            }
        });

        btnStop.setText("Stop");
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });

        btnChangeDir.setText("<< >>");
        btnChangeDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnChangeDirActionPerformed(evt);
            }
        });

        labelDir.setText("  ?");
        labelDir.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnStop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnChangeDir, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cbLight)
                            .addComponent(cbHorn))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblAddress)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(comboSelAddress, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSliderSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelDir, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(comboSelAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblAddress))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelDir)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jSliderSpeed, javax.swing.GroupLayout.Alignment.TRAILING, 0, 0, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(40, 40, 40)
                                .addComponent(cbLight)
                                .addGap(8, 8, 8)
                                .addComponent(cbHorn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnStop)
                                .addGap(18, 18, 18)
                                .addComponent(btnChangeDir)))))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        savePrefs();
        sx.removeFromPlist(lok_adr);
        ThrottleUIInstance--;
        tl.remove(this);
    }//GEN-LAST:event_formWindowClosing

    private void comboSelAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboSelAddressActionPerformed
        int new_adr = Integer.parseInt(comboSelAddress.getSelectedItem().toString());
        if (new_adr != lok_adr) {
            // only if changed
            sx.removeFromPlist(lok_adr);
        }
        lok_adr = new_adr;
        System.out.println("lok adr=" + lok_adr);
        sx.addToPlist(lok_adr);
        update();  // re-init nach neuer Adresse
    }//GEN-LAST:event_comboSelAddressActionPerformed

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        speed = 0;
        jSliderSpeed.setValue(0);
        updateLoco();
    }//GEN-LAST:event_btnStopActionPerformed

    private void btnChangeDirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnChangeDirActionPerformed
        // TODO add your handling code here:
        // change dir
        toggleDirection();
    }//GEN-LAST:event_btnChangeDirActionPerformed

    private void jSliderSpeedPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jSliderSpeedPropertyChange
        //
    }//GEN-LAST:event_jSliderSpeedPropertyChange

    private void jSliderSpeedStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSliderSpeedStateChanged
        //  if (!jSliderSpeed.getValueIsAdjusting()) {
        // dies kann irritierend sein, wenn man am Slider dreht, aber (noch)
        // nichts passiert - deshalb wieder disabled.
            speed = jSliderSpeed.getValue();
            updateLoco();
        //  }
    }//GEN-LAST:event_jSliderSpeedStateChanged

    private void cbLightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbLightActionPerformed
        if (cbLight.getModel().isSelected()) {
            licht = true;
        } else {
            licht = false;
        }
        updateLoco();
    }//GEN-LAST:event_cbLightActionPerformed

    private void cbHornActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbHornActionPerformed
        if (cbHorn.getModel().isSelected()) {
            horn = true;
        } else {
            horn = false;
        }
        updateLoco();
    }//GEN-LAST:event_cbHornActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnChangeDir;
    private javax.swing.JButton btnStop;
    private javax.swing.JCheckBox cbHorn;
    private javax.swing.JCheckBox cbLight;
    private javax.swing.JComboBox<String> comboSelAddress;
    private javax.swing.JSlider jSliderSpeed;
    private javax.swing.JLabel labelDir;
    private javax.swing.JLabel lblAddress;
    // End of variables declaration//GEN-END:variables

    private void updateLoco() {
        sxi.sendLoco(lok_adr, speed, licht, forward,  horn);
    }

    private void update() {
        // initial werte lesen aus sxData
        int ld = sxData[lok_adr][0];
        speed = (ld & 0x1F);
        horn = ((ld & 0x80) == 0x80);
        licht = ((ld & 0x40) == 0x40);
        forward = !((ld & 0x20) == 0x20);

        if (horn) {
            cbHorn.setSelected(true);
        } else {
            cbHorn.setSelected(false);
        }
        if (licht) {
            cbLight.setSelected(true);
        } else {
            cbLight.setSelected(false);
        }
        if (forward == true) {
            labelDir.setText(">>");
        } else {
            labelDir.setText("<<");
        }
        jSliderSpeed.setValue(speed);
    }
    
    private void toggleDirection() {
        if (forward == true) {
            forward = false;
            labelDir.setText("<<");
        } else {
            forward = true;
            labelDir.setText(">>");
        }
        updateLoco();
    }
    private void speedUp() {
        // initial werte lesen aus sxData
        int ld = sxData[lok_adr][0];
        speed = (ld & 0x1F);
        if (speed < 31) speed = speed+1;
        jSliderSpeed.setValue(speed);
    }
    
    private void speedDown() {
        // initial werte lesen aus sxData
        int ld = sxData[lok_adr][0];
        speed = (ld & 0x1F);
        if (speed > 0) speed = speed-1;
        jSliderSpeed.setValue(speed);
    }

     private void savePrefs() {
        // fuer SX3 Programm, zB Belegtmelder: Instanz-Nummer (Klassenvariable) mit im
        // String, um mehrere Fensterpositionen zu speichern
        // auch SX-adresse jeweils speichern.
        String myInst = "TH"+myInstance;
        prefs.putInt(myInst+"windowX", getX());
        prefs.putInt(myInst+"windowY", getY());
        prefs.putInt(myInst+"adr",lok_adr);

    }

    private void loadPrefs() {
        // reload the positions for the right instance
        String myInst="TH"+myInstance;
        if (DEBUG) System.out.println("loading Prefs for:"+myInst);

        setLocation(prefs.getInt(myInst+"windowX", 200), prefs.getInt(myInst+"windowY", 200));
        lok_adr=prefs.getInt(myInst+"adr",1);
        //setSize(prefs.getInt("width", 500),prefs.getInt("height",300));
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
