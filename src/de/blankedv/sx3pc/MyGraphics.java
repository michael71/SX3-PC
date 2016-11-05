/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.blankedv.sx3pc;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 *
 * @author mblank
 */
public class MyGraphics {

    public static Icon imageRotate90(Icon ci) {

        int w = ci.getIconWidth();
        int h = ci.getIconHeight();
        Image i1 = iconToImage(ci);
// Create a BufferedImage of the same size as the Image

        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();    // Get a Graphics2D object
        g.drawImage(i1, 0, 0, null);      // Draw the Image data into the BufferedImage

        // aus Image eine ImageIcon machen:
        ImageIcon ic = new ImageIcon(rotateImage(bi, 90));

        return ic;

    }

    public static Icon imageRotate(Icon ci, int n) {
        // rotate n*90 degrees
        int w = ci.getIconWidth();
        int h = ci.getIconHeight();
        Image i1 = iconToImage(ci);
// Create a BufferedImage of the same size as the Image

        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();    // Get a Graphics2D object
        g.drawImage(i1, 0, 0, null);      // Draw the Image data into the BufferedImage

        // aus Image eine ImageIcon machen:
        ImageIcon ic = new ImageIcon(rotateImage(bi, 90*n));

        return ic;
    }

    private static BufferedImage rotateImage(BufferedImage src, double degrees) {
        AffineTransform affineTransform = AffineTransform.getRotateInstance(
                Math.toRadians(degrees),
                src.getWidth() / 2,
                src.getHeight() / 2);
        BufferedImage rotatedImage = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        Graphics2D g = (Graphics2D) rotatedImage.getGraphics();
        g.setTransform(affineTransform);
        g.drawImage(src, 0, 0, null);

        return rotatedImage;
    }

    public static Image iconToImage(Icon icon) {
        if (icon instanceof ImageIcon) {
            return ((ImageIcon) icon).getImage();
        } else {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            BufferedImage image = gc.createCompatibleImage(w, h);
            Graphics2D g = image.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return image;
        }
    }

}
