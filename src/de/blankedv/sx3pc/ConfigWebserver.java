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
import java.net.URI;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class ConfigWebserver {

    String fileName = "";
    String locoFileName = "";
    HttpServer server;
    Preferences appPrefs;

    public ConfigWebserver(Preferences p, int port) throws Exception {
        appPrefs = p;
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
            System.out.println("URI=" + requestURI.getPath());
            String fname = "";
            try {
                String fileName = appPrefs.get("configfilename", "-keiner-");
                if (requestURI.getPath().contains("config")) {
                    if (fileName.equals("-keiner-")) {
                        response = " ERROR - no config file selected";
                        fname = "?";
                    } else {
                        fname = fileName;
                        response = new String(Files.readAllBytes(Paths.get(fname)));
                    }
                } else {
                    response = "ERROR:  use URL :8000/config";
                }
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (IOException ex) {
                System.out.println("Config File: " + fname + " not found - only :8000/config allowed");
                response = "ERROR FILE NOT FOUND OR WRONG URL: " + fname + " - only :8000/config allowed";
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
