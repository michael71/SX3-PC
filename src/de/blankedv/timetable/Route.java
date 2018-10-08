package de.blankedv.timetable;

import de.blankedv.sx3pc.SXAddrAndBits;
import de.blankedv.sx3pc.SXUtils;
import static de.blankedv.timetable.Vars.*;
import java.util.ArrayList;

/**
 * Class Route stores a complete route, which contains sensors, signals and
 * turnouts. Offending allRoutes are calculated automatically (defined as all
 allRoutes which also set one of the turnouts). In addition offending allRoutes can
 also be defined in the config file (needed for crossing allRoutes, which cannot
 be found automatically)

 adapted from lanbahnpanel (android software)
 *
 * @author mblank
 *
 */
public class Route extends PanelElement {

    String routeString = "";
    String sensorsString = "";
    String offendingString = ""; // comma separated list of adr's of offending
    // allRoutes

    // sensors turnout activate for the display of this route
    private ArrayList<PanelElement> rtSensors = new ArrayList<>();

    // signals of this route
    private ArrayList<RouteSignal> rtSignals = new ArrayList<>();

    // turnouts of this route
    private ArrayList<RouteTurnout> rtTurnouts = new ArrayList<>();

    // offending allRoutes
    private ArrayList<Route> rtOffending = new ArrayList<>();

    /**
     * constructs a route
     *
     * @param id unique identifier (int)
     * @param route string for route setting like "770,1;720,2"
     * @param allSensors string for sensors like "2000,2001,2002"
     * @param offending string with offending allRoutes, separated by comma
     */
    public Route(int routeAddr, String route, String allSensors,
            String offending) {
 
        super("RT", routeAddr);
        state = RT_INACTIVE;
        // these strings are written back to config file.
        this.routeString = route;
        this.sensorsString = allSensors;
        this.offendingString = offending;

        lastUpdateTime = System.currentTimeMillis(); // store for resetting
        
        if (DEBUG) {
            System.out.println(" creating route id/adr=" + adr);
        }

        // route = "750,1;751,2" => set 750 turnout 1 and 751 turnout value 2
        String[] routeElements = route.split(";");
        for (int i = 0; i < routeElements.length; i++) {
            String reInfo[] = routeElements[i].split(",");

            PanelElement pe = PanelElement.getByAddress(Integer.parseInt(reInfo[0]));

            // if this is a signal, then add to my signal list "rtSignals"
            if (pe != null) {
                if (pe.isSignal()) {
                    if (reInfo.length == 3) {  // route signal with dependency
                        rtSignals.add(new RouteSignal(pe,
                                Integer.parseInt(reInfo[1]),
                                Integer.parseInt(reInfo[2])));
                    } else {
                        rtSignals.add(new RouteSignal( pe, Integer
                                .parseInt(reInfo[1])));
                    }

                } else if (pe.isTurnout()) {
                    rtTurnouts.add(new RouteTurnout(pe,
                            Integer.parseInt(reInfo[1])));
                }
            }
        }
        if (DEBUG) {
            System.out.println(" " + rtSignals.size() + " signals");
        }
        if (DEBUG) {
            System.out.println(" " + rtTurnouts.size() + " turnouts");
        }

        // format for sensors: just a list of addresses, seperated by comma ","
        String[] sensorAddresses = allSensors.split(",");
        for (int i = 0; i < sensorAddresses.length; i++) {
            // add the matching elements turnout sensors list
            for (PanelElement pe : panelElements) {
                if (pe.isSensor()) {
                    if (pe.getAdr() == Integer.parseInt(sensorAddresses[i])) {
                        rtSensors.add(pe);
                    }
                }
            }
        }
        if (DEBUG) {
            System.out.println(" " + rtSensors.size() + " sensors");
        }

        String[] offRoutes = offendingString.split(",");
        for (int i = 0; i < offRoutes.length; i++) {
            for (Route rt : allRoutes) {
                try {
                    int offID = Integer.parseInt(offRoutes[i]);
                    if ((rt.adr == offID) && (rt.state == RT_ACTIVE)) {
                        rtOffending.add(rt);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        //	if (DEBUG)
        //		Log.d(TAG, rtOffending.size() + " offending allRoutes in config");
    }

    public void clear() {
        lastUpdateTime = System.currentTimeMillis(); // store for resetting
        // automatically
        if (DEBUG) {
            System.out.println(" clearing route id=" + adr);
        }

        // deactivate sensors
        for (PanelElement se : rtSensors) {
           int st= se.setBit1(false);
           /*
           if (se.secondaryAdr != INVALID_INT) {
                // for track-control "route lighting"
               // TODO currently not shown on SXBUS !!! serialIF.send(LNUtil.makeOPC_SW_REQ(se.secondaryAdr - 1, 1, 1));
            } */
           LbUtils.updateLanbahnData(se.adr, st);
        }

        // set signals turnout red
        for (RouteSignal rs : rtSignals) {
            rs.signal.setState(STATE_RED);
            
            // TODO set on SXBUS
            //serialIF.send(LNUtil.makeOPC_SW_REQ(rs.signal.adr - 1, 0, 1));
            LbUtils.updateLanbahnData(rs.signal.adr, rs.signal.getState());
        }

        // TODO unlock turnouts
        /*
		 * for (RouteTurnout to : rtTurnouts) { 
		 *     String cmd = "U " + to.turnout.adr;
		 *     sendQ.add(cmd); 
		 * }
         */

        // notify that route was cleared
        LbUtils.updateLanbahnData(adr, RT_INACTIVE);

    }

    public void clearOffendingRoutes() {
        if (DEBUG) {
            System.out.println(" clearing (active) offending Routes");
        }
        String[] offRoutes = offendingString.split(",");
        for (int i = 0; i < offRoutes.length; i++) {
            for (Route rt : allRoutes) {
                try {
                    int offID = Integer.parseInt(offRoutes[i]);
                    if ((rt.adr == offID) && (rt.state == RT_ACTIVE)) {
                        rt.clear();
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    public boolean offendingRouteActive() {
        if (DEBUG) {
            System.out.println(" checking for (active) offending Routes");
        }
        String[] offRoutes = offendingString.split(",");
        for (int i = 0; i < offRoutes.length; i++) {
            for (Route rt : allRoutes) {
                try {
                    int offID = Integer.parseInt(offRoutes[i]);
                    if ((rt.adr == offID) && (rt.state == RT_ACTIVE)) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        return false;
    }

    public boolean set() {


        if (DEBUG) {
            System.out.println(" setting route id=" + adr);
        }

        if (offendingRouteActive()) {
            if (DEBUG) {
                System.out.println(" offending route active");
            }
            return false;
        }

        lastUpdateTime = System.currentTimeMillis(); // store for resetting
        // automatically

        clearOffendingRoutes();
        
        // notify that route is set
        LbUtils.updateLanbahnData(adr, RT_ACTIVE);

        // activate sensors
        for (PanelElement se : rtSensors) {
            int st = se.setBit1(true);
            if (se.secondaryAdr != INVALID_INT) {
                // for track-control "route lighting"
               // TODO for SXBUS serialIF.send(LNUtil.makeOPC_SW_REQ(se.secondaryAdr - 1, 0, 1));
            }
            LbUtils.updateLanbahnData(se.adr, st);
        }

        // set signals
        for (RouteSignal rs : rtSignals) {
            int d = rs.dynamicValueToSetForRoute();
            SXAddrAndBits sxab = SXUtils.lbAddr2SX(rs.signal.adr);
            if (d == 0) {   // TODO multi-aspect - only red and green are used at the moment
                SXUtils.clearBitSxData(sxab.sxAddr, sxab.bit);
            } else {
                SXUtils.clearBitSxData(sxab.sxAddr, sxab.bit);
            }
            LbUtils.updateLanbahnData(rs.signal.adr, d);
        }
        // set and // TODO lock turnouts
        for (RouteTurnout rtt : rtTurnouts) {
            int d = rtt.valueToSetForRoute;   // can be only 1 or 0
            SXAddrAndBits sxab = SXUtils.lbAddr2SX(rtt.turnout.adr);
             if (d == 0) {  
                SXUtils.clearBitSxData(sxab.sxAddr, sxab.bit);
            } else {
                SXUtils.clearBitSxData(sxab.sxAddr, sxab.bit);
            }
            LbUtils.updateLanbahnData(rtt.turnout.adr, rtt.valueToSetForRoute);
        }   
         
        return true;
    }

    public boolean isActive() {
        return (state == RT_ACTIVE);
    }

    protected class RouteSignal {

        PanelElement signal;
        private int valueToSetForRoute;
        private int depFrom;

        RouteSignal(PanelElement se, int value) {
            signal = se;
            valueToSetForRoute = value;
            depFrom = INVALID_INT;
        }

        RouteSignal(PanelElement se, int value, int dependentFrom) {
            signal = se;
            valueToSetForRoute = value;
            depFrom = dependentFrom;
        }

        int dynamicValueToSetForRoute() {
            // set standard value if not green
            if ((depFrom == INVALID_INT) || (valueToSetForRoute != STATE_GREEN)) {
                return valueToSetForRoute;
            } else {
                // if standard-value == GREEN then check the other signal, which
                // this signal state depends on
                PanelElement depPe = PanelElement.getByAddress(depFrom);
                if (depPe.getState() == STATE_RED) {
                    // if other signal red, then set to yellow
                    return STATE_YELLOW;
                } else {
                    return valueToSetForRoute;
                }

            }
        }
    }

    protected void updateDependencies() {
        // update signals which have a dependency from another signal
        // set signals
        for (RouteSignal rs : rtSignals) {
            if (rs.depFrom != INVALID_INT) {
                if (rs.signal.getState() != rs.dynamicValueToSetForRoute()) {
                    rs.signal.state = rs.dynamicValueToSetForRoute();
                    LbUtils.updateLanbahnData(rs.signal.adr, rs.signal.state);
                }
            }
        }

    }

    protected class RouteTurnout {

        PanelElement turnout;
        int valueToSetForRoute;

        RouteTurnout(PanelElement te, int value) {
            turnout = te;
            valueToSetForRoute = value;
        }
    }

    public static void auto() {
        // check for auto reset of allRoutes
        for (Route rt : allRoutes) {
            if (((System.currentTimeMillis() - rt.lastUpdateTime) > AUTO_CLEAR_ROUTE_TIME_SEC * 1000L)
                    && (rt.state == RT_ACTIVE)) {
                rt.clear();
            }
            // update dependencies
            if (rt.state == RT_ACTIVE) {
                rt.updateDependencies();
            }
        }

    }

    

    public void addOffending(Route rt2) {
        // check if not already contained in offending string
        if (!rtOffending.contains(rt2)) {
            rtOffending.add(rt2);
        }
    }

    public String getOffendingString() {

        StringBuilder sb = new StringBuilder("");
        for (Route r : rtOffending) {
            if (sb.length() == 0) {
                sb.append(r.adr);
            } else {
                sb.append(",");
                sb.append(r.adr);
            }
        }
        /*		if (sb.length() == 0)
			Log.d(TAG, "route adr=" + adr + " has no offending allRoutes.");
		else
			Log.d(TAG, "route adr=" + adr + " has offending allRoutes with ids="
					+ sb.toString()); */
        return sb.toString();

    }

    public static void calcOffendingRoutes() {
        for (Route rt : allRoutes) {
            for (RouteTurnout t : rt.rtTurnouts) {
                // iterate over all turnouts of rt and check, if another route
                // activates the same turnout to a different position 
                for (Route rt2 : allRoutes) {
                    if (rt.adr != rt2.adr) {
                        for (RouteTurnout t2 : rt2.rtTurnouts) {
                            if ((t.turnout.adr == t2.turnout.adr)
                                    && (t.valueToSetForRoute != t2.valueToSetForRoute)) {
                                rt.addOffending(rt2);
                                break;
                            }

                        }
                    }
                }
            }
            rt.offendingString = rt.getOffendingString();
        }

    }
    
    public static Route getFromAddress(int a) {
        for (Route r : allRoutes) {
            if (r.adr == a) return r;
        }
        return null;
    }
}
