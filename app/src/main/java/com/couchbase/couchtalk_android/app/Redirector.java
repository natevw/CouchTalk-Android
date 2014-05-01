package com.couchbase.couchtalk_android.app;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import Acme.Serve.Serve;

/**
 * Created by natevw on 4/30/14.
 */
public class Redirector implements Runnable {
    private Serve httpServer;
    private int listenPort = Serve.DEF_PORT;

    public Redirector() {
        httpServer = new Serve();
        httpServer.addServlet("/", new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
                String origPortPrefixed = String.format(":%d", listenPort);
                String host = req.getHeader("Host").replace(origPortPrefixed, ":59840");
                String target = String.format("%s://%s/couchtalk/_design/app/index.html", req.getScheme(), host);
                res.sendRedirect(target);
            }
        });
    }

    @Override
    public void run() {
        httpServer.serve();
    }

    public int getListenPort() {
        return listenPort;
    }
}
