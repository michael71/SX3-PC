/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Random;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import static de.blankedv.sx3pc.InterfaceUI.running;

/**
 *
 * @author mblank
 */
public class RegisterJMDNSService implements Runnable {

    private final static String SXNET_TYPE = "_sxnet._tcp.local.";
    private final static String LANBAHN_TYPE = "_lanbahn._udp.local.";
    private final static String SXCONFIG_TYPE = "_sxconfig._tcp.local.";
    
    private String type;
    private String servicetype;
    private String servicename;
    private int port;
    private InetAddress ip;
    private JmDNS jmdns;
    private ServiceInfo serviceInfo;

    
    public RegisterJMDNSService(String t, int p, InetAddress ip ) {
        this.type = t;
        this.port = p;
        if (t.equals("sxnet")) {
            servicetype = SXNET_TYPE;
        } else if (t.equals("lanbahn")) {
            servicetype = LANBAHN_TYPE;
        } else if (t.equals("sxconfig")) {
            servicetype = SXCONFIG_TYPE;
        }else {
            System.out.println("wrong service type in RegisterService");
        }
        this.ip = ip;
    }

    public void run() {
        Random random = new Random();
        int id = random.nextInt(8999) + 1000;
        servicename = type + id;     
        
        try {
            jmdns = JmDNS.create(ip,servicename);
        } catch (IOException ex) {
            //Logger.getLogger(RegisterJMDNSService.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error, could not create JmDns.");
            return;
        }
        System.out.println("JmDNS for service="+servicename);
    

        final HashMap<String, String> values = new HashMap<String, String>();
        servicename = type + id;
        values.put("name", servicename);
        values.put("version", "1.0");

        
        serviceInfo = ServiceInfo.create(servicetype, servicename, port, 0, 0, values);
        try {
            jmdns.registerService(serviceInfo);
            System.out.println("announcing sxnet service '" + servicename+"'");
        } catch (IOException ex) {
            //Logger.getLogger(RegisterJMDNSService.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error, could not announce sxnet service '" + servicename+"'");
            return;
        }

        while (running) {
            // just wait for end.
            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
                break;
            }
        }

        System.out.println("Closing JmDNS, service="+servicename);
        //jmdns.unregisterService(serviceInfo);
        jmdns.unregisterService(serviceInfo);
        try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
   
        }
        try {
            jmdns.close();
        } catch (IOException ex) {
            System.out.println("Error: "+ex.getMessage());
            //Logger.getLogger(RegisterJMDNSService.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
