/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

 /*
 * MonitorUI.java
 *
 * Created on 06.04.2011, 19:33:39
 */
package de.blankedv.sx3pc;

import java.awt.Color;
import java.awt.Container;
import java.util.prefs.Preferences;
import static de.blankedv.sx3pc.MainUI.*;

/**
 *
 * @author mblank
 */
public class MonitorUI extends javax.swing.JFrame {

    private static final long serialVersionUID = 5313123456415L;
    static final int ROWS = 16;
    static final int COLS = 14; // *2
    private int[] oldSxData = new int[N_SX];
    private int index = 0;  //SX0 or SX1 or SIM
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());

    /**
     * Creates new form MonitorUI
     */
    public MonitorUI() {   // used for SX0 and SX1 and SIM display
        initComponents();

        loadPrefs();
        initTable1();  // adressen schreiben

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
        jTable1 = new javax.swing.JTable();

        setTitle("SX Monitor");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jTable1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jTable1.setFont(jTable1.getFont().deriveFont(jTable1.getFont().getSize()-2f));
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "A", "12345678", "A", "12345678", "A", "12345678", "A", "12345678", "A", "12345678", "A", "12345678", "A", "12345678"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setRowSelectionAllowed(false);
        jTable1.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setResizable(false);
            jTable1.getColumnModel().getColumn(1).setResizable(false);
            jTable1.getColumnModel().getColumn(2).setResizable(false);
            jTable1.getColumnModel().getColumn(3).setResizable(false);
            jTable1.getColumnModel().getColumn(4).setResizable(false);
            jTable1.getColumnModel().getColumn(5).setResizable(false);
            jTable1.getColumnModel().getColumn(6).setResizable(false);
            jTable1.getColumnModel().getColumn(7).setResizable(false);
        }
        jTable1.getColumnModel().getColumn( 0 ).setPreferredWidth( 10);
        jTable1.getColumnModel().getColumn( 2 ).setPreferredWidth( 10);
        jTable1.getColumnModel().getColumn( 4 ).setPreferredWidth( 10);
        jTable1.getColumnModel().getColumn( 6 ).setPreferredWidth( 10);
        jTable1.getColumnModel().getColumn( 8 ).setPreferredWidth( 10);
        jTable1.getColumnModel().getColumn( 10 ).setPreferredWidth( 10);
        jTable1.getColumnModel().getColumn( 12 ).setPreferredWidth( 15);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 806, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        savePrefs();
        sxmon = null;  // to enable opening a MonitorUI window again in Interface UI
    }//GEN-LAST:event_formWindowClosing

    public void update() {

        int count = 0;
        boolean redflag = false;
        // set adresses
        for (int i = 1; i < COLS; i = i + 2) {
            for (int j = 0; j < ROWS; j++) {
                if (sxData.get(count) != oldSxData[count]) {
                    redflag = true;
                } else {
                    redflag = false;
                }
                jTable1.setValueAt(SXBinaryString(sxData.get(count), redflag), j, i);
                // if (count > 106/2)  System.out.println("S="+SXBinaryString(sxData[count])+".");
                oldSxData[count] = sxData.get(count);
                count++;

            }
        }
    }

    private String SXBinaryString(int data, boolean redColorFlag) {
        StringBuffer s;
        int pos = 0;

        if (redColorFlag) {
            s = new StringBuffer("<html><p bgcolor='#FF8800'>00000000</p></html>");
            pos = 27; // position where to insert data
        } else {
            if (data == 0) {
                s = new StringBuffer("00000000");
            } else {
                s = new StringBuffer("<html><p bgcolor='#FFFF00'>00000000</p></html>");
                pos = 27;
            }

        }

        // Selectrix Schreibweise LSB vorn !!
        if ((data & 0x01) == 0x01) {
            s.setCharAt(0 + pos, '1');
        }
        if ((data & 0x02) == 0x02) {
            s.setCharAt(1 + pos, '1');
        }
        if ((data & 0x04) == 0x04) {
            s.setCharAt(2 + pos, '1');
        }
        if ((data & 0x08) == 0x08) {
            s.setCharAt(3 + pos, '1');
        }
        if ((data & 0x10) == 0x10) {
            s.setCharAt(4 + pos, '1');
        }
        if ((data & 0x20) == 0x20) {
            s.setCharAt(5 + pos, '1');
        }
        if ((data & 0x40) == 0x40) {
            s.setCharAt(6 + pos, '1');
        }
        if ((data & 0x80) == 0x80) {
            s.setCharAt(7 + pos, '1');
        }
        return s.toString();
    }

    private void initTable1() {
        int count = 0;
        // set adresses
        for (int i = 0; i < COLS; i = i + 2) {
            for (int j = 0; j < ROWS; j++) {
                jTable1.setValueAt(count, j, i);
                oldSxData[count] = 0;
                count++;
            }
        }
    }

    public void savePrefs() {
        // fuer SX3 Programm, zB Belegtmelder: Instanz-Nummer (Klassenvariable) mit im
        // String, um mehrere Fensterpositionen zu speichern
        // auch SX-adresse jeweils speichern.

        prefs.putInt("monitorwindowX", getX());
        prefs.putInt("monitorwindowY", getY());

    }

    private void loadPrefs() {
        // reload the positions for the right instance

        setLocation(prefs.getInt("monitorwindowX", 200), prefs.getInt("monitorwindowY", 200));

    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
