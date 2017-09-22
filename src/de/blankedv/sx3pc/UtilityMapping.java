/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.InterfaceUI.INVALID_INT;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static de.blankedv.sx3pc.InterfaceUI.panelName;
import static de.blankedv.sx3pc.InterfaceUI.allLanbahnSXPairs;
import static de.blankedv.sx3pc.InterfaceUI.allLocoNetSXPairs;

/**
 * utility function for the mapping of lanbahn addresses to SX addresses (and
 * bits) and vice versa
 *
 * @author mblank
 */
public class UtilityMapping {

    // TODO second sxbit
    private static final boolean CFG_DEBUG = true;

    private static String sxMode = "";

    public static void init(String configfilename) {

        readXMLConfigFile(configfilename);

        // example UtilityMapping      
        // allLanbahnSXPairs.add(new LanbahnSXPair(722, 72, 2));
        // allLanbahnSXPairs.add(new LanbahnSXPair(721, 74, 1));
    }

    // return a list of lanbahn mappings which have been changed
    // for a given SX address and SXdata change
    public static ArrayList<LanbahnSXPair> getChangedLanbahnFromSXByte(int sxaddr, int sxbyte, int oldSxbyte) {
        // TODO !!!!
        ArrayList<LanbahnSXPair> lbvs = new ArrayList<>();
        for (LanbahnSXPair ls : allLanbahnSXPairs) {
            if (ls.sxAddr == sxaddr) { // address matches, now look for bits
                //if (ls.getLBValueFromSXByte(sxbyte) != ls.getLBValueFromSXByte(oldSxbyte)) {
                //    if (!lbvs.contains(ls)) {
                //        lbvs.add(ls);
                 //   }

                //}
                if (!lbvs.contains(ls)) {  // add all potentially changed lanbahn channels
                         lbvs.add(ls);
                }
            }
        }
        if (sxaddr == 70) {
            for (LanbahnSXPair ls:lbvs) {
                 System.out.println("sxadr70 / changed="+ls.toString());
            }
           
        }
        return lbvs;  // can be empty
    }

    public static SXAddrAndBits getSXAddrAndBitsFromLanbahnAddr(int lbaddr) {
        for (LanbahnSXPair lbx:allLanbahnSXPairs) {
            if (lbx.lbAddr == lbaddr) {
                // matching entry found
                return new SXAddrAndBits(lbx.sxAddr, lbx.sxBit, lbx.nBit);
            }
        }
        return null;
    }
    
    // code template taken from lanbahnPanel
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
//<sxmapping adr="822" sxadr="81" sxbit="5" nbit="2" /> 2 sxbits are used 5 (low) and 6 (high)

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
            System.out.println("config: " + items.getLength() + " lanbahn-sx mappings");
        }
        for (int i = 0; i < items.getLength(); i++) {
            LanbahnSXPair tmp = parseSXMapping(items.item(i));
            if (tmp != null) {
                System.out.println("map: " + tmp.toString());
                allLanbahnSXPairs.add(tmp);
            }
        }
        
        // look for loco net sensor mappings
        items = root.getElementsByTagName("lnmapping");
        if (CFG_DEBUG) {
            System.out.println("config: " + items.getLength() + " loconet-sx mappings");
        }
        for (int i = 0; i < items.getLength(); i++) {
            LocoNetSXPair tmp = parseLocoNetMapping(items.item(i));
            if (tmp != null) {
                System.out.println("map: " + tmp.toString());
                allLocoNetSXPairs.add(tmp);
            }
        }
    }
    // code template from lanbahnPanel

    private static LanbahnSXPair parseSXMapping(Node item) {

        LanbahnSXPair sxmap = new LanbahnSXPair();

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
                sxmap.sxBit = getPositionNode(theAttribute);
            } else if (theAttribute.getNodeName().equals("nbit")) {
                sxmap.nBit = getPositionNode(theAttribute);
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

    private static LocoNetSXPair parseLocoNetMapping(Node item) {

        LocoNetSXPair sxmap = new LocoNetSXPair();

        NamedNodeMap attributes = item.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node theAttribute = attributes.item(i);
            // if (CFG_DEBUG_PARSING) Log.d(TAG,theAttribute.getNodeName() + "=" +
            // theAttribute.getNodeValue());
            if (theAttribute.getNodeName().equals("adr")) {
                sxmap.lnAddr = getPositionNode(theAttribute);
            } else if (theAttribute.getNodeName().equals("sxadr")) {
                sxmap.sxAddr = getPositionNode(theAttribute);
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
