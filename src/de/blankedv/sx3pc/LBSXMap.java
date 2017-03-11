/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.InterfaceUI.INVALID_INT;
import static de.blankedv.sx3pc.InterfaceUI.lbsx;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static de.blankedv.sx3pc.InterfaceUI.panelName;
/**
 * utility function for the mapping of lanbahn addresses to SX addresses (and
 * bits) and vice versa
 *
 * @author mblank
 */
public class LBSXMap {

    // TODO second sxbit
    private static final boolean CFG_DEBUG = true;
    
    private static String sxMode = "";

    public static void init(String configfilename) {

        readXMLConfigFile(configfilename);

        // example LBSXMap      
        // lbsx.add(new LBSX(722, 72, 2));
        // lbsx.add(new LBSX(721, 74, 1));
    }

    public static int getLanbahn(int sxaddr, int sxbit) {
        for (LBSX ls : lbsx) {
            if ((ls.sxAddr == sxaddr) && (ls.sxBit == sxbit)) {
                return ls.lbAddr;
            }
        }
        return INVALID_INT; // => no mapping
    }

    public static SXAddrAndBit getSX(int lbAddr) {
        for (LBSX ls : lbsx) {
            if (ls.lbAddr == lbAddr) {
                return new SXAddrAndBit(ls.sxAddr, ls.sxBit, ls.sxBit2);
            }
        }
        return new SXAddrAndBit(INVALID_INT, 0);   // => no mapping
    }
    
// code template from lanbahnPanel
    private static String readXMLConfigFile(String fname) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            System.out.println("ParserConfigException Exception - " + e1.getMessage());
            return "ParserConfigException";
        }
        Document doc;
        try {
            doc = builder.parse(new File(fname));
            parseMappings(doc);
        } catch (SAXException e) {
            System.out.println("SAX Exception - " + e.getMessage());
            return "SAX Exception - " + e.getMessage();
        } catch (IOException e) {
            System.out.println("IO Exception - " + e.getMessage());
            return "IO Exception - " + e.getMessage();
        }

        return "";
    }

    // code template from lanbahnPanel
    private static void parseMappings(Document doc) {
        // assemble new ArrayList of tickets.
        //<layout-config>
//<panel name="Lonstoke West 2">
//<mode sx="on" />
//<sxmapping adr="811" sxadr="81" sxbit="1" />
//<sxmapping adr ...

        NodeList items;
        Element root = doc.getDocumentElement();

        items = root.getElementsByTagName("panel");
        panelName = parsePanelName(items.item(0));
        if (CFG_DEBUG) {
            System.out.println("panelName =" + panelName);
        }

        items = root.getElementsByTagName("mode");
        sxMode = parseMode(items.item(0));
        if (CFG_DEBUG) {
            System.out.println("sx mode =" + sxMode);
        }
        // NamedNodeMap attributes = item.getAttributes();
        // Node theAttribute = attributes.items.item(i);

        // look for TrackElements - this is the lowest layer
        items = root.getElementsByTagName("sxmapping");
        if (CFG_DEBUG) {
            System.out.println("config: " + items.getLength() + " sxmappings");
        }
        for (int i = 0; i < items.getLength(); i++) {
            LBSX tmp = parseSXMapping(items.item(i));
            if (tmp != null) {
                System.out.println("map=" + tmp.toString());
                lbsx.add(tmp);
            }
        }
    }
    // code template from lanbahnPanel

    private static LBSX parseSXMapping(Node item) {

        LBSX sxmap = new LBSX();

        NamedNodeMap attributes = item.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node theAttribute = attributes.item(i);
            // if (CFG_DEBUG_PARSING) Log.d(TAG,theAttribute.getNodeName() + "=" +
            // theAttribute.getNodeValue());
            if (theAttribute.getNodeName().equals("adr")) {
                sxmap.lbAddr = getPositionNode(theAttribute);
            } else if (theAttribute.getNodeName().equals("sxadr")) {
                sxmap.sxAddr = getPositionNode(theAttribute);
            } else if (theAttribute.getNodeName().equals("sxbit")) {
                int tmp = getPositionNode(theAttribute);
                if (tmp < 10) {  // only a single bit used for this mapping
                    if ((tmp >= 1) && (tmp <= 8)) {
                        sxmap.sxBit = tmp;
                        sxmap.sxBit2 = INVALID_INT;
                    }
                } else if ((tmp >= 11) && (tmp <= 87)) {
                    // two sx bits used for this mapping
                    int tmp1 = tmp % 10;
                    if ((tmp1 >= 1) && (tmp1 <= 8)) {
                        sxmap.sxBit2 = tmp1;
                    }
                    int tmp2 = tmp / 10;
                    if ((tmp2 >= 1) && (tmp2 <= 8)) {
                        sxmap.sxBit = tmp2;
                    }

                }
            } else {
                if (CFG_DEBUG) {
                    System.out.println(
                            "unknown attribute " + theAttribute.getNodeName()
                            + " in config file");
                }
            }
        }

        if (sxmap.isValid()) {
            return sxmap;
        } else {
            return null;
        }

    }

    // code from lanbahnPanel
    private static int getPositionNode(Node a) {
        return Integer.parseInt(a.getNodeValue());
    }
    // code from lanbahnPanel

    private static int getValue(String s) {
        float b = Float.parseFloat(s);
        return (int) b;
    }
    // code from lanbahnPanel

    private static String parsePanelName(Node item) {
        NamedNodeMap attributes = item.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node theAttribute = attributes.item(i);

            if (theAttribute.getNodeName().equals("name")) {
                String name = theAttribute.getNodeValue();
                return name;

            }
        }
        return "";
    }

    private static String parseMode(Node item) {
        NamedNodeMap attributes = item.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node theAttribute = attributes.item(i);

            if (theAttribute.getNodeName().equals("sx")) {
                String name = theAttribute.getNodeValue();
                return name;

            }
        }
        return "";
    }
}
