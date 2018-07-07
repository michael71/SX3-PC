/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx3pc;

/**
 *
 * @author mblank
 */
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import static de.blankedv.sx3pc.MainUI.myip;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigWebserver {

    String fileName = "";
    static final String jmdnsService = "sxconfig";
    HttpServer server;

    public ConfigWebserver(String fName, int port) throws Exception {
        fileName = fName;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/config", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();

        new Thread(new RegisterJMDNSService(jmdnsService, port, myip.get(0))).start();

        /* GUI:
    JFileChooser chooser = new JFileChooser();
    FileNameExtensionFilter filter = new FileNameExtensionFilter(
        "xml", "XML");
    chooser.setFileFilter(filter);
    int returnVal = chooser.showOpenDialog(parent);
    if(returnVal == JFileChooser.APPROVE_OPTION) {
       System.out.println("You chose to open this file: " +
            chooser.getSelectedFile().getName());
    }   */
    }

    public void stop() {
        server.stop(0);

    }

    class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) {
            String response;
            try {
                response = new String(Files.readAllBytes(Paths.get(fileName)));
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (IOException ex) {
                System.out.println("Config File: " + fileName + " not found");
                response = "Config File: " + fileName + " not found";
                try {
                    t.sendResponseHeaders(200, response.length());
                     OutputStream os = t.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } catch (IOException ex1) {
                    // ERROR msg printed already.
                }
               
            }

        }
    }

}
