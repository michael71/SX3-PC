package de.blankedv.timetable;

import static de.blankedv.sx3pc.MainUI.DEBUG;
//import static de.blankedv.ln3pc.Variables.*;
import static de.blankedv.timetable.Vars.*;
import java.util.ArrayList;

/**
 * composite route, i.e. a list of allRoutes which build a new "compound" route
 * is only a helper for ease of use, no more functionality than the "simple" 
 * Route (i.e. a CompRoute contains a "list of Routes")
 * 
 * @author mblank
 *
 */
public class CompRoute extends PanelElement {
    
    String routesString = ""; // identical to config string

    // route is comprised of a list of allRoutes
    private ArrayList<Route> myroutes = new ArrayList<>();

    /**
     * constructs a composite route
     *
     *
     */
    public CompRoute(int routeAddr, String sRoutes) {
        super("CR",routeAddr);
        setState(RT_INACTIVE);
        // this string written back to config file.
        this.routesString = sRoutes;
        lastUpdateTime = System.currentTimeMillis(); // store for resetting 
        if (DEBUG) {
            System.out.println("creating comproute id=" + routeAddr);
        }

        // allRoutes = "12,13": these allRoutes need to be activated.
        String[] iID = routesString.split(",");
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < iID.length; i++) {
            int routeID = Integer.parseInt(iID[i]);
            for (Route rt : allRoutes) {
                try {
                    if (rt.getAdr() == routeID) {
                        myroutes.add(rt);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        if (DEBUG) {
            System.out.println(myroutes.size() + " routes in this route.");
        }

    }

    public void clearOffendingRoutes() {
        if (DEBUG) {
            System.out.println(" clearing (active) offending Routes");
        }
        for (Route rt : myroutes) {
            rt.clearOffendingRoutes();

        }
    }

    public boolean set() {

        if (DEBUG) {
            System.out.println(" setting comproute id=" + getAdr());
        }
        lastUpdateTime = System.currentTimeMillis();
        setState(RT_ACTIVE);
        // check if all routes can be set successfully
        boolean res = true;
        for (Route rt : myroutes) {
            res = rt.set();
            if (res == false) {
                if (DEBUG) {
                    System.out.println("ERROR cannot set comproute id=" + getAdr() + " because route=" + rt.getAdr() + " cannot be set.");
                }
                return false;  // cannot set comproute.
            }
            // else continue with next route
        }
        return res;
    }

     public static void auto() {
        // check for auto reset of allCompRoutes
        // this function is only needed for the lanbahn-value display, because the individual single routes,
        // which are set by a compound route, are autocleared by the "Route.auto()" function
        for (CompRoute rt : allCompRoutes) {
            if (((System.currentTimeMillis() - rt.lastUpdateTime) > AUTO_CLEAR_ROUTE_TIME_SEC * 1000L)
                    && (rt.getState() == RT_ACTIVE)) {
                rt.setState(RT_INACTIVE);
             }

        }

    }
     
     public static CompRoute getFromAddress(int a) {
        for (CompRoute cr : allCompRoutes) {
            if (cr.getAdr() == a) return cr;
        }
        return null;
    }
}
