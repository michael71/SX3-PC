/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.timetable;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import de.blankedv.sx3pc.LbData;
import static de.blankedv.sx3pc.MainUI.INVALID_INT;
import static de.blankedv.sx3pc.MainUI.SXMAX;
import de.blankedv.sx3pc.SXAddrAndBits;
import de.blankedv.sx3pc.SXUtils;

import static de.blankedv.timetable.Vars.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
 * utility function for the mapping of lanbahn addresses to DCC addresses (and
 * bits) and vice versa
 *
 * @author mblank
 */
public class ReadConfig {

    private static final boolean CFG_DEBUG = true;

    // code template taken from lanbahnPanel
    public static String readXML(String fname) {
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
            parsePEsAndTimetable(doc);
            // sort the trips by ID
            Collections.sort(allTrips, (a, b) -> b.compareTo(a));

            parseRoutes(doc); // can be done only after all turnouts, signals etc have been read

            // elements have been read
            Route.calcOffendingRoutes(); // calculate offending routes
            parseCompRoutes(doc); // can be done only after all routes have been read

        } catch (SAXException e) {
            System.out.println("SAX Exception - " + e.getMessage());
            return "SAX Exception - " + e.getMessage();
        } catch (IOException e) {
            System.out.println("IO Exception - " + e.getMessage());
            return "IO Exception - " + e.getMessage();
        } catch (Exception e) {
            System.out.println("other Exception - " + e.getMessage());
            return "other Exception - " + e.getMessage();
        }

        return "OK";
    }

    // code template from lanbahnPanel
    private static void parsePEsAndTimetable(Document doc) {
        // assemble new ArrayList of tickets.
        //<layout-config>
//<panel name="Lonstoke West 2">
//<signal x="290" y="100" x2="298" y2="100" adr="76s3" nbit="2" />   
//     ==> map lanbahn value at address 763 to 2 dcc addresses: 763 (low) and 764 (high bit)

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
        items = root.getElementsByTagName("turnout");
        if (CFG_DEBUG) {
            System.out.println("config: " + items.getLength() + " turnouts");
        }
        for (int i = 0; i < items.getLength(); i++) {
            addPanelElement("T", items.item(i));
        }

        items = root.getElementsByTagName("signal");
        if (CFG_DEBUG) {
            System.out.println("config: " + items.getLength() + " signals");
        }

        for (int i = 0; i < items.getLength(); i++) {
            addPanelElement("Si", items.item(i));
        }

        items = root.getElementsByTagName("sensor");
        if (CFG_DEBUG) {
            System.out.println("config: " + items.getLength() + " sensors");
        }
        for (int i = 0; i < items.getLength(); i++) {
            addPanelElement("BM", items.item(i));
        }

        items = root.getElementsByTagName("doubleslip");
        if (CFG_DEBUG) {
            System.out.println("config: " + items.getLength() + " doubleslips");
        }
        for (int i = 0; i < items.getLength(); i++) {
            addPanelElement("DS", items.item(i));
        }

        items = root.getElementsByTagName("trip");
        for (int i = 0; i < items.getLength(); i++) {
            Trip tr = parseTrip(items.item(i));
            if (tr != null) {
                System.out.println("trip id=" + tr.id);
                allTrips.add(tr);
            }
        }

        items = root.getElementsByTagName("timetable");
        for (int i = 0; i < items.getLength(); i++) {
            Timetable ti = parseTimetable(items.item(i));
            if (ti != null) {
                System.out.println("timetable id=" + ti.id);
                allTimetables.add(ti);
            }
        }

    }

    // code from lanbahnPanel
    private static int getIntValueOfNode(Node a) {
        return Integer.parseInt(a.getNodeValue());
    }
    // code from lanbahnPanel

    /* private static float getFloatValueOfNode(String s) {
        float b = Float.parseFloat(s);
        return  b;
    } */
    private static ArrayList<Integer> parseAddressArray(Node a) {
        NamedNodeMap attributes = a.getAttributes();

        // determine type - OLD (sxadr/sxbit/nbit) or NEW (lanbahn addresses, sep. by comma)
        boolean newFormat = false;
        for (int i = 0; i < attributes.getLength(); i++) {
            Node theAttribute = attributes.item(i);
            if (theAttribute.getNodeName().equals("adr")) {
                newFormat = true;
                break;
            }
        }
        if (newFormat) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node theAttribute = attributes.item(i);
                if (theAttribute.getNodeName().equals("adr")) {
                    String s = theAttribute.getNodeValue();
                    s = s.replace(".", "");
                    //s = s.replace("\\s+", "");
                    String[] sArr = s.split(",");
                    ArrayList<Integer> iArr = new ArrayList<>();

                    for (String s2 : sArr) {
                        int addr = INVALID_INT;
                        try {
                            addr = Integer.parseInt(s2);
                        } catch (NumberFormatException ex) {
                        }
                        iArr.add(addr);
                    }
                    return iArr;
                } else {
                    // check, if the XML file uses the old "sxadr"/"sxbit" combination
                }
            }
        } else {
            SXAddrAndBits sx = parseSXMapping(a);
            if (sx == null) {
                return null;
            }
            // calculate lbaddress from sxaddr, sxbit, nbit
            if ((SXUtils.isValidSXAddress(sx.sxAddr) && SXUtils.isValidSXBit(sx.sxBit))) {
                ArrayList<Integer> iArr = new ArrayList<>();
                iArr.add(sx.sxAddr * 10 + sx.sxBit);
                if (sx.nBit == 2) {
                    iArr.add(sx.sxAddr * 10 + sx.sxBit + 1);
                }
                return iArr;
            }
        }
        return null;
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

    private static SXAddrAndBits parseSXMapping(Node item) {

        SXAddrAndBits sxmap = new SXAddrAndBits();

        NamedNodeMap attributes = item.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node theAttribute = attributes.item(i);
            // if (CFG_DEBUG_PARSING) Log.d(TAG,theAttribute.getNodeName() + "=" +
            // theAttribute.getNodeValue());
            if (theAttribute.getNodeName().equals("sxadr")) {
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
            if ((sxmap.sxAddr != INVALID_INT) && (sxmap.sxAddr <= SXMAX)
                    && (sxmap.sxBit != INVALID_INT)
                    && (sxmap.sxBit >= 1) && (sxmap.sxBit <= 7)) {
                // we have a valid sx address
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

    private static Trip parseTrip(Node item) {

        Trip t = new Trip();

        NamedNodeMap attributes = item.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node theAttribute = attributes.item(i);
            if (theAttribute.getNodeName().equals("id")) {
                t.id = getIntValueOfNode(theAttribute);
            } else if (theAttribute.getNodeName().equals("route")) {
                t.route = theAttribute.getNodeValue();
            } else if (theAttribute.getNodeName().equals("sens1")) {
                t.sens1 = getIntValueOfNode(theAttribute);
            } else if (theAttribute.getNodeName().equals("sens2")) {
                t.sens2 = getIntValueOfNode(theAttribute);
            } else if (theAttribute.getNodeName().equals("loco")) {
                t.locoString = theAttribute.getNodeValue();
            } else if (theAttribute.getNodeName().equals("stopdelay")) {
                t.stopDelay = getIntValueOfNode(theAttribute);
            }
        }

        // check if Trip information is complete
        if ((t.id != INVALID_INT)
                && (!t.route.isEmpty())
                && (t.sens1 != INVALID_INT)
                && (t.sens2 != INVALID_INT)
                && (t.convertLocoData())) {
            // we have the minimum info needed

            if (t.stopDelay == INVALID_INT) {
                t.stopDelay = 0;
            }
            return t;
        } else {
            System.out.println("invalid trip, id=" + t.id);
            return null;
        }
    }

    private static Timetable parseTimetable(Node item) {

        int id = INVALID_INT;
        String sTime = "";
        String sTrip = "";
        String sNext = "";

        NamedNodeMap attributes = item.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node theAttribute = attributes.item(i);
            if (theAttribute.getNodeName().equals("id")) {
                id = getIntValueOfNode(theAttribute);
            } else if (theAttribute.getNodeName().equals("time")) {
                sTime = theAttribute.getNodeValue();
            } else if (theAttribute.getNodeName().equals("trip")) {
                sTrip = theAttribute.getNodeValue();
            } else if (theAttribute.getNodeName().equals("next")) {
                sNext = theAttribute.getNodeValue();
            }
        }

        // check if Trip information is complete
        if ((id != INVALID_INT)
                && (!sTime.isEmpty())
                && (!sTrip.isEmpty())) {
            // we have the minimum info needed

            return new Timetable(id, sTime, sTrip, sNext);
        } else {
            System.out.println("invalid Timetable, id=" + id);
            return null;
        }
    }

    private static void parseRoutes(Document doc) {

        NodeList items;
        Element root = doc.getDocumentElement();

        // items = root.getElementsByTagName("panel");
        // look for allRoutes - this is the lowest layer
        items = root.getElementsByTagName("route");
        if (DEBUG) {
            System.out.println("config: " + items.getLength() + " routes");
        }
        for (int i = 0; i < items.getLength(); i++) {
            parseRoute(items.item(i));
        }

    }

    private static void parseRoute(Node item) {
        int id = INVALID_INT;
        String route = null, sensors = null;
        String offending = ""; // not mandatory

        NamedNodeMap attributes = item.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node theAttribute = attributes.item(i);
            // if (DEBUG_PARSING) System.out.println(TAG+theAttribute.getNodeName() + "=" +
            // theAttribute.getNodeValue());
            if (theAttribute.getNodeName().equals("id")) {
                id = Integer.parseInt(theAttribute.getNodeValue());
            } else if (theAttribute.getNodeName().equals("route")) {
                route = theAttribute.getNodeValue();
            } else if (theAttribute.getNodeName().equals("sensors")) {
                sensors = theAttribute.getNodeValue();
            } else if (theAttribute.getNodeName().equals("offending")) {
                offending = theAttribute.getNodeValue();
            }
        }

        // check for mandatory and valid input data
        if (id == INVALID_INT) {
            // missing info, log error
            System.out.println("missing id= info in route definition");
            return;
        } else if (route == null) {
            System.out.println("missing route= info in route definition");
            return;
        } else if (sensors == null) {
            System.out.println("missing sensors= info in route definition");
            return;
        } else {
            // everything is o.k.
            Route rt = new Route(id, route, sensors, offending);
            LbUtils.createLanbahnData(id, 1, "RT");   // 1 bit , route
            panelElements.add(rt);
            allRoutes.add(rt);
        }

    }

    private static void parseCompRoutes(Document doc) {
        // assemble new ArrayList of tickets.
        NodeList items;
        Element root = doc.getDocumentElement();

        // look for comp allRoutes - this is the lowest layer
        items = root.getElementsByTagName("comproute");
        if (DEBUG) {
            System.out.println("config: " + items.getLength() + " comproutes");
        }
        for (int i = 0; i < items.getLength(); i++) {
            parseCompRoute(items.item(i));
        }
    }

    private static void parseCompRoute(Node item) {
        //
        int id = INVALID_INT;
        int btn1 = INVALID_INT;
        int btn2 = INVALID_INT;
        String routes = null;

        NamedNodeMap attributes = item.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node theAttribute = attributes.item(i);
            // if (DEBUG_PARSING) System.out.println(TAG+theAttribute.getNodeName() + "=" +
            // theAttribute.getNodeValue());
            if (theAttribute.getNodeName().equals("id")) {
                id = Integer.parseInt(theAttribute.getNodeValue());
            } else if (theAttribute.getNodeName().equals("routes")) {
                routes = theAttribute.getNodeValue();
            }
        }

        // check for mandatory and valid input data
        if (id == INVALID_INT) {
            // missing info, log error
            System.out.println("missing id= info in route definition");
            return;
        } else if (routes == null) {
            System.out.println("missing routes= info in route definition");
            return;
        } else {
            // everything is o.k.
            CompRoute cr = new CompRoute(id, routes);
            LbUtils.createLanbahnData(id, 1, "CR");
            panelElements.add(cr);
            allCompRoutes.add(cr);
        }

    }

    private static void addPanelElement(String type, Node a) {
        ArrayList<Integer> addressArr = parseAddressArray(a);
        if (addressArr != null) {
            int lba = addressArr.get(0);
            switch (addressArr.size()) {
                case 1:
                    panelElements.add(new PanelElement(type, lba));
                    if (!SXUtils.isValidSXAddress(lba/10)) { // is a virtual address
                        LbUtils.createLanbahnData(addressArr.get(0), 1, type);
                    }
                    break;
                case 2:
                    int secLba = addressArr.get(1);
                    panelElements.add(new PanelElement(type, lba, secLba));
                    if (!SXUtils.isValidSXAddress(lba/10) || !SXUtils.isValidSXAddress(secLba/10)) {
                        // one of the addresses is a virtual address, must create lanbahn data,
                        // even if the first address is in SXadr range
                        LbUtils.createLanbahnData(lba, 2, type);
                    }
                    break;
                default:
                    System.out.println("ERROR in XML definition, more than 2 adresses");
            }
        } else {
            System.out.println("ERROR in XML definition, no address found");
        }

    }
}
