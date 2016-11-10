/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * WeichenUI.java
 *
 * Created on 08.04.2011, 14:24:32
 */

package de.blankedv.sx3pc;
import java.util.prefs.Preferences;
import java.util.List;
import java.util.ArrayList;
import static de.blankedv.sx3pc.InterfaceUI.*;   // DAS SX interface.


/**
 *
 * @author mblank
 */
public class WeichenUI extends javax.swing.JFrame {
    private static final long serialVersionUID = 534251256456411L;
    private int w_adr;      // weichen adresse
    private int data;       // daten (8 bit) dieser adresse

    // Bilden einer Liste, damit wir später an alle Fenster dieses Typs die
    // Updates verschicken können
    static List<WeichenUI> wl = new ArrayList<WeichenUI>();  

    private int myInstance;
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    static int WeichenUIInstance = 0;

    public static void updateAll() {
       for ( WeichenUI w: wl) {
           w.update();
       }
    }

    public static void saveAllPrefs() {
       for ( WeichenUI w: wl) {
           w.savePrefs();
       }
    }

    /** Creates new form WeichenUI */
    public WeichenUI() {
        initComponents();
        myInstance = WeichenUIInstance++;
        loadPrefs(); //myInstance is used here.
        if (DEBUG) System.out.println("constr. w adr="+w_adr+"/SX"+sxbusControl);
        jComboBox1.setSelectedIndex(w_adr);  // index starts from 0, addresses start also at 0
        wl.add(this);
        this.setTitle(bundle.getString("TurnoutEtc")+ "  [SX"+sxbusControl+"]");
        lblAddress.setText(bundle.getString("Address"));
        
        update(); // from SX Bus data
        this.setVisible(true);
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
        jComboBox1 = new javax.swing.JComboBox<>();
        jCheckBox1 = new javax.swing.JCheckBox();
        jCheckBox2 = new javax.swing.JCheckBox();
        jCheckBox3 = new javax.swing.JCheckBox();
        jCheckBox4 = new javax.swing.JCheckBox();
        jCheckBox5 = new javax.swing.JCheckBox();
        jCheckBox6 = new javax.swing.JCheckBox();
        jCheckBox7 = new javax.swing.JCheckBox();
        jCheckBox8 = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        lblSum = new javax.swing.JLabel();

        setTitle("Weichen, Signale, AUX");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        lblAddress.setText("Adr");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "100", "101", "102", "103", "104", "105", "106", "107", "108", "109", "110", "111" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });

        jCheckBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox3ActionPerformed(evt);
            }
        });

        jCheckBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox4ActionPerformed(evt);
            }
        });

        jCheckBox5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox5ActionPerformed(evt);
            }
        });

        jCheckBox6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox6ActionPerformed(evt);
            }
        });

        jCheckBox7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox7ActionPerformed(evt);
            }
        });

        jCheckBox8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox8ActionPerformed(evt);
            }
        });

        jLabel1.setText("  1");

        jLabel2.setText("  2");

        jLabel3.setText("  3");

        jLabel4.setText("  4");

        jLabel5.setText("  5");

        jLabel6.setText("  6");

        jLabel7.setText("  7");

        jLabel8.setText("  8");

        lblSum.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblSum.setText("sum");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(lblSum, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBox1)
                    .addComponent(jLabel1))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBox2)
                    .addComponent(jLabel2))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBox3)
                    .addComponent(jLabel3))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBox4)
                    .addComponent(jLabel4))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBox5)
                    .addComponent(jLabel5))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBox6)
                    .addComponent(jLabel6))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBox7)
                    .addComponent(jLabel7))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBox8)
                    .addComponent(jLabel8))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(16, 16, 16)
                    .addComponent(lblAddress)
                    .addContainerGap(345, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jCheckBox8)
                        .addComponent(jCheckBox7)
                        .addComponent(jCheckBox6)
                        .addComponent(jCheckBox5)
                        .addComponent(jCheckBox4)
                        .addComponent(jCheckBox3)
                        .addComponent(jCheckBox2)
                        .addComponent(jCheckBox1))
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(jLabel2)
                        .addComponent(jLabel3)
                        .addComponent(jLabel4)
                        .addComponent(jLabel5)
                        .addComponent(jLabel6)
                        .addComponent(jLabel7)
                        .addComponent(jLabel8))
                    .addComponent(lblSum, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(25, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(22, 22, 22)
                    .addComponent(lblAddress)
                    .addContainerGap(52, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        int new_adr = Integer.parseInt(jComboBox1.getSelectedItem().toString());
        if (new_adr != w_adr) {
            // only if changed
            sx.removeFromPlist(w_adr);
        }
        w_adr = new_adr;
        if (DEBUG) System.out.println("w adr="+w_adr);
        sx.addToPlist(w_adr);  //only needed for 1 SX channel !
        update();
}//GEN-LAST:event_jComboBox1ActionPerformed

    private void jCheckBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox3ActionPerformed
       if (jCheckBox3.getModel().isSelected()) {
            data =  data | 0x04;
        } else {
            data = (data & 0xFB);
        }
       sendeWeiche();
    }//GEN-LAST:event_jCheckBox3ActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        if (jCheckBox1.getModel().isSelected()) {
            data = data | 0x01;
        } else {
            data = (data & 0xFE);
        }
        sendeWeiche();
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        if (jCheckBox2.getModel().isSelected()) 
        { 
            data =  data | 0x02;
        } else {
            data = (data & 0xFD);
        }
        sendeWeiche();
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    private void jCheckBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox4ActionPerformed
        if (jCheckBox4.getModel().isSelected()) {
            data =  data | 0x08;
        } else {
            data = (data & 0xF7);
        }
       sendeWeiche();
    }//GEN-LAST:event_jCheckBox4ActionPerformed

    private void jCheckBox5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox5ActionPerformed
        if (jCheckBox5.getModel().isSelected()) {
            data =  data | 0x10;
        } else {
            data = (data & 0xEF);
        }
       sendeWeiche();
    }//GEN-LAST:event_jCheckBox5ActionPerformed

    private void jCheckBox6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox6ActionPerformed
        if (jCheckBox6.getModel().isSelected()) {
            data =  data | 0x20;
        } else {
            data = (data & 0xDF);
        }
       sendeWeiche();
    }//GEN-LAST:event_jCheckBox6ActionPerformed

    private void jCheckBox7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox7ActionPerformed
        if (jCheckBox7.getModel().isSelected()) {
            data = data | 0x40;
        } else {
            data = data & 0xBF;
        }
       sendeWeiche();
    }//GEN-LAST:event_jCheckBox7ActionPerformed

    private void jCheckBox8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox8ActionPerformed
        if (jCheckBox8.getModel().isSelected()) {
            data = data | 0x80;
        } else {
            data = (data & 0x7F);
        }
       sendeWeiche();
    }//GEN-LAST:event_jCheckBox8ActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        savePrefs();
        WeichenUIInstance--;
        sx.removeFromPlist(w_adr);
        wl.remove(this);
    }//GEN-LAST:event_formWindowClosing

    private void sendeWeiche() {
         sxi.sendAccessory(w_adr, data);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JCheckBox jCheckBox4;
    private javax.swing.JCheckBox jCheckBox5;
    private javax.swing.JCheckBox jCheckBox6;
    private javax.swing.JCheckBox jCheckBox7;
    private javax.swing.JCheckBox jCheckBox8;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel lblAddress;
    private javax.swing.JLabel lblSum;
    // End of variables declaration//GEN-END:variables

    private void update() {
        data = sxData[w_adr][sxbusControl];

        if ((data & 0x01) == 0x01) { jCheckBox1.setSelected(true);  } else { jCheckBox1.setSelected(false); }
        if ((data & 0x02) == 0x02) { jCheckBox2.setSelected(true);  } else { jCheckBox2.setSelected(false); }
        if ((data & 0x04) == 0x04) { jCheckBox3.setSelected(true);  } else { jCheckBox3.setSelected(false); }
        if ((data & 0x08) == 0x08) { jCheckBox4.setSelected(true);  } else { jCheckBox4.setSelected(false); }
        if ((data & 0x10) == 0x10) { jCheckBox5.setSelected(true);  } else { jCheckBox5.setSelected(false); }
        if ((data & 0x20) == 0x20) { jCheckBox6.setSelected(true);  } else { jCheckBox6.setSelected(false); }
        if ((data & 0x40) == 0x40) { jCheckBox7.setSelected(true);  } else { jCheckBox7.setSelected(false); }
        if ((data & 0x80) == 0x80) { jCheckBox8.setSelected(true);  } else { jCheckBox8.setSelected(false); }
        lblSum.setText("Val="+data);
    }

    private void savePrefs() {
        // fuer SX3 Programm, zB Belegtmelder: Instanz-Nummer (Klassenvariable) mit im
        // String, um mehrere Fensterpositionen zu speichern
        // auch SX-adresse jeweils speichern.
        String myInst = "WS"+myInstance;
        prefs.putInt(myInst+"windowX", getX());
        prefs.putInt(myInst+"windowY", getY());
        prefs.putInt(myInst+"adr",w_adr);

    }

    private void loadPrefs() {
        // reload the positions for the right instance
        String myInst="WS"+myInstance;
        if (DEBUG) System.out.println("loading Prefs for:"+myInst);

        setLocation(prefs.getInt(myInst+"windowX", 200), prefs.getInt(myInst+"windowY", 200));
        w_adr=prefs.getInt(myInst+"adr",80);

    }
}
