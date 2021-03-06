/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * SensorUI.java
 *
 * Created on 07.04.2011, 14:13:18
 */

package de.blankedv.sx3pc;

import java.util.prefs.Preferences;
import java.util.List;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import static de.blankedv.sx3pc.MainUI.*;

/**
 *
 * @author mblank
 */
public class SensorUI extends javax.swing.JFrame {
    private static final long serialVersionUID = 5313123456413L;
    private int s_adr;
    private ImageIcon green, red;
    private int sum = 0;  //decimal sum of the set bits

    static List<SensorUI> sl = new ArrayList<SensorUI>();  //Liste, damit alle Fenster die Updates bekommen
    static int sensorUIInstances = 0;

    private int myInstance;  // zählt Fenster hoch (und runter), verwendet für Prefs
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());

    public static void updateAll() {
       for ( SensorUI s: sl) {
           s.update();
       }
    }

    public static void saveAllPrefs() {
       for ( SensorUI s: sl) {
           s.savePrefs();
       }
    }

    /** Creates new form SensorUI */
    public SensorUI() {
        initComponents();
        green = new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/greendot.png"));
        red = new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/reddot.png"));
        sl.add(this);
        myInstance = sensorUIInstances++;
        loadPrefs(); //myInstance is used here.
        jComboBox1.setSelectedIndex(s_adr-1);  // index starts from 0, addresses +1
        if (DEBUG) System.out.println("s adr="+s_adr);
        this.setTitle("Sensor");
        lblAddress.setText("Addr");
       
        this.setVisible(true);
        this.update();
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
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        lblSum = new javax.swing.JLabel();

        setTitle("Belegtmelder");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        lblAddress.setText("Addr");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "100", "101", "102", "103", "104", "105", "106", "107", "108", "109", "110", "111" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/greendot.png"))); // NOI18N

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/greendot.png"))); // NOI18N

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/greendot.png"))); // NOI18N

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/greendot.png"))); // NOI18N

        jLabel6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/greendot.png"))); // NOI18N

        jLabel7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/greendot.png"))); // NOI18N

        jLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/greendot.png"))); // NOI18N

        jLabel9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/blankedv/sx3pc/icons/greendot.png"))); // NOI18N

        jLabel10.setText("   1");

        jLabel1.setText("   2");

        jLabel11.setText("   3");

        jLabel12.setText("  4");

        jLabel13.setText("  5");

        jLabel14.setText("   6");

        jLabel15.setText("  7");

        jLabel16.setText("  8");

        lblSum.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblSum.setText("Val");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblAddress)
                        .addGap(13, 13, 13)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblSum, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel12)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel13)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel9))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel15)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel16)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblAddress)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7)
                    .addComponent(jLabel8)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jLabel1)
                    .addComponent(jLabel11)
                    .addComponent(jLabel12)
                    .addComponent(jLabel13)
                    .addComponent(jLabel14)
                    .addComponent(jLabel15)
                    .addComponent(jLabel16)
                    .addComponent(lblSum))
                .addContainerGap(23, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        int new_adr = Integer.parseInt(jComboBox1.getSelectedItem().toString());

        s_adr = new_adr;
        if (DEBUG) System.out.println("s adr="+s_adr);

        update();
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        savePrefs();
        sensorUIInstances--;
         sl.remove(this);
    }//GEN-LAST:event_formWindowClosing

   
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel lblAddress;
    private javax.swing.JLabel lblSum;
    // End of variables declaration//GEN-END:variables

    private void update() {
        //
        int data = sxData.get(s_adr);
        if ((data & 0x01) != 0) jLabel2.setIcon(red) ;
        else jLabel2.setIcon(green) ;
        if ((data & 0x02) != 0) jLabel3.setIcon(red) ;
        else jLabel3.setIcon(green) ;
        if ((data & 0x04) != 0)  jLabel4.setIcon(red) ;
        else jLabel4.setIcon(green) ;
        if ((data & 0x08) != 0)  jLabel5.setIcon(red) ;
        else jLabel5.setIcon(green) ;
        if ((data & 0x10) != 0)  jLabel6.setIcon(red) ;
        else jLabel6.setIcon(green) ;
        if ((data & 0x20) != 0)  jLabel7.setIcon(red) ;
        else jLabel7.setIcon(green) ;
        if ((data & 0x40) != 0)  jLabel8.setIcon(red) ;
        else jLabel8.setIcon(green) ;
        if ((data & 0x80) != 0)  jLabel9.setIcon(red) ;
        else jLabel9.setIcon(green) ;
        lblSum.setText("Val="+data);
    }

     private void savePrefs() {
        // fuer SX3 Programm, zB Belegtmelder: Instanz-Nummer (Klassenvariable) mit im
        // String, um mehrere Fensterpositionen zu speichern
        // auch SX-adresse jeweils speichern.
        String myInst = "BM"+ myInstance;

        prefs.putInt(myInst+"windowX", getX());
        prefs.putInt(myInst+"windowY", getY());
        prefs.putInt(myInst+"adr",s_adr);

    }

    private void loadPrefs() {
        // reload the positions for the right instance
        String myInst="BM"+ myInstance;
        if (DEBUG) System.out.println("loading Prefs for:"+myInst);

        setLocation(prefs.getInt(myInst+"windowX", 200), prefs.getInt(myInst+"windowY", 200));
        s_adr=prefs.getInt(myInst+"adr",81);

    }
}
