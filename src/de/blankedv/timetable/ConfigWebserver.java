/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.timetable;

/**
 *
 * @author mblank
 */
import com.sun.net.httpserver.Headers;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import static de.blankedv.sx3pc.MainUI.myip;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
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
            OutputStream os = null;
            try {
                String fileName = appPrefs.get("configfilename", "-keiner-");
                Headers h = t.getResponseHeaders();
                os = t.getResponseBody();
                if (requestURI.getPath().contains("config")) {
                    if (fileName.equals("-keiner-")) {
                        response = " ERROR! config file not yet selected ";
                        fname = "?";
                        h.add("Content-Type", "text/html ; charset=utf-8");
                    } else {
                        fname = fileName;
                        response = new String(Files.readAllBytes(Paths.get(fname)));
                        h.add("Content-Type", "text/xml ; charset=utf-8");
                    }

                    t.sendResponseHeaders(200, response.length());

                    os.write(response.getBytes());
                } else if (requestURI.getPath().contains("lanbahnpanel.apk")) {

                    File file = new File("dist/apk/lanbahnpanel.apk");
                    if (file.exists()) {
                        h.add("Content-Type", "application/vnd.android.package-archive");
                        byte[] bytearray = new byte[(int) file.length()];
                        FileInputStream fis = new FileInputStream(file);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        bis.read(bytearray, 0, bytearray.length);

                        // ok, we are ready to send the response.
                        t.sendResponseHeaders(200, file.length());

                        os.write(bytearray, 0, bytearray.length);
                    } else {
                        response = "ERROR! lanbahnpanel.apk does not exist in dist/apk directory";
                        h.add("Content-Type", "text/html ; charset=utf-8");
                        t.sendResponseHeaders(200, response.length());
                        os.write(response.getBytes());
                    }
                } else {
                    response = "ERROR! use URL http://serverip:8000/config or http://serverip:8000/lanbahnpanel.apk";
                    h.add("Content-Type", "text/html ; charset=utf-8");
                    t.sendResponseHeaders(200, response.length());
                    os.write(response.getBytes());
                }

            } catch (IOException ex) {
                System.out.println("ERROR " + ex.getMessage());
                response = "ERROR";
                try {
                    Headers h = t.getResponseHeaders();
                    h.add("Content-Type", "text/html ; charset=utf-8");
                    t.sendResponseHeaders(200, response.length());
                    os = t.getResponseBody();
                    os.write(response.getBytes());
                } catch (IOException ex1) {
                    System.out.println("ERROR " + ex1.getMessage());
                }

            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(ConfigWebserver.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }

}
