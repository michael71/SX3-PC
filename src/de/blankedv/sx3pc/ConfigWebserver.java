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
import java.net.URI;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigWebserver {

    String fileName = "";
    String locoFileName = "";
    HttpServer server;

    public ConfigWebserver(String fName, String lName, int port) throws Exception {
        fileName = fName;
        locoFileName = lName;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        
    }

    public void stop() {
        server.stop(0);

    }

    class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) {
            String response;
            URI requestURI = t.getRequestURI();
            System.out.println("URI="+requestURI.getPath());
            String fname = "";
            try {
                if (requestURI.getPath().contains("config")) {
                    fname = fileName;
                } else if (requestURI.getPath().contains("loco")) {
                    fname = locoFileName;
                } else {
                    fname = "___";
                }
                response = new String(Files.readAllBytes(Paths.get(fname)));
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (IOException ex) {
                System.out.println("Config/Loco File: " + fname + " not found - only :8000/config and :8000/loco allowed");
                response = "Config/Loco File: " + fname + " not found - only :8000/config and :8000/loco allowed";
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
