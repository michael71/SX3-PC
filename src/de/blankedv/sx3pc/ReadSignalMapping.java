/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import static de.blankedv.sx3pc.MainUI.INVALID_INT;
import static de.blankedv.sx3pc.MainUI.SXMAX;
import static de.blankedv.sx3pc.SXnetSession.allSignalMappings;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * utility function for the mapping of lanbahn addresses to SX addresses (and
 * bits) and vice versa
 *
 * @author mblank
 */
public class ReadSignalMapping {

    // TODO second sxbit
    private static final boolean CFG_DEBUG = true;

    public static void init(String configfilename) {

        readXMLConfigFile(configfilename);

        // example UtilityMapping      
        // allLanbahnSXPairs.add(new LanbahnSXPair(722, 72, 2));
        // allLanbahnSXPairs.add(new LanbahnSXPair(721, 74, 1));
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
            parseSignals(doc);
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
    private static void parseSignals(Document doc) {
        // assemble new ArrayList of tickets.
        //<layout-config>
//<panel name="Lonstoke West 2">
//<signal x="290" y="100" x2="298" y2="100" adr="763" nbit="2" />   
//     ==> map lanbahn value at address 763 to 2 sxbits, 76.3 (low) and 76.4 (high)

        NodeList items;
        Element root = doc.getDocumentElement();

        items = root.getElementsByTagName("panel");
        if (items.getLength() == 0) {
            return;
        }

        String panelProtocol = parsePanelAttribute(items.item(0), "protocol");

        if (CFG_DEBUG) {
            System.out.println("panelProtocol =" + panelProtocol);
        }

        // NamedNodeMap attributes = item.getAttributes();
        // Node theAttribute = attributes.items.item(i);
        // look for TrackElements - this is the lowest layer
        items = root.getElementsByTagName("signal");
        if (CFG_DEBUG) {
            System.out.println("config: " + items.getLength() + " signals");
        }
        for (int i = 0; i < items.getLength(); i++) {
            SignalMapping tmp = parseSXMapping(items.item(i));
            if ((tmp != null) && (tmp.lbAddr != INVALID_INT)) {
                System.out.println("signal mapping: " + tmp.toString());
                allSignalMappings.add(tmp);
            }
        }

    }
    // code template from lanbahnPanel

    private static SignalMapping parseSXMapping(Node item) {

        SignalMapping sxmap = new SignalMapping();

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
            }
        }

        if (sxmap.nBit == 2) {
            // currently implemented only for 4 aspect signals
            // either the sxadr/sxbit are defined or the (lanbahn-)adr
            if (sxmap.lbAddr != INVALID_INT) {
                // lanbahn address is defined, calculate sx
                sxmap.sxAddr = sxmap.lbAddr / 10;
                sxmap.sxBit = sxmap.lbAddr % 10;
                if ((sxmap.sxBit < 1) || (sxmap.sxBit > 7)) {
                    // doesnt work
                    System.out.println("invalid config data, sxBit=" + sxmap.sxBit + " for lanbahn-adr=" + sxmap.lbAddr);
                    return null;
                }
                return sxmap;
            } else if ((sxmap.sxAddr != INVALID_INT) && (sxmap.sxAddr <= SXMAX)
                    && (sxmap.sxBit != INVALID_INT)
                    && (sxmap.sxBit >= 1) && (sxmap.sxBit <= 7)) {
                // we have a valid sx address
                sxmap.lbAddr = sxmap.sxAddr * 10 + sxmap.sxBit;
                return sxmap;
            } else {
                System.out.println("invalid config data, sxAddr=" + sxmap.sxAddr + " sxBit=" + sxmap.sxBit + " nBit=" + sxmap.nBit);
            }
        }
        return null;
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

    private static String parsePanelAttribute(Node item, String att) {
        NamedNodeMap attributes = item.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node theAttribute = attributes.item(i);

            if (theAttribute.getNodeName().equals(att)) {
                String attrib = theAttribute.getNodeValue();
                return attrib;

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
