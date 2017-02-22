package com.bugreport.sslviassl.server;

import com.bugreport.sslviassl.LineReader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Very simple HTTP server
 */
public class SimpleHttpServer {
    private InetSocketAddress mAddress;
    private Thread mDispatcherThread;
    private ServerSocket mServerSocket;

    public SimpleHttpServer(InetSocketAddress address){
        mAddress = address;
    }

    public InetSocketAddress getAddress(){
        if (mServerSocket == null) return null;
        return ((InetSocketAddress) mServerSocket.getLocalSocketAddress());
    }

    private void readRequestHeader(Socket client) throws IOException {
        LineReader reader = new LineReader(client.getInputStream());

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) {
                return;
            }
        }
    }

    private void sendResponse(Socket client) throws IOException {
        String header = "HTTP/1.1 200 OK\r\n" +
                "Host: " + mAddress.getHostName() + ":" + mAddress.getPort() + "\r\n" +
                "Connection: close\r\n\r\n";
        String data = "Working";
        String response = header + data;
        client.getOutputStream().write(response.getBytes());
        client.getOutputStream().flush();
    }

    public void start(){
        mDispatcherThread = new Thread(){
            @Override
            public void run() {
                try {
                    mServerSocket = new ServerSocket();
                    mServerSocket.bind(mAddress);

                    while (!isInterrupted()) {
                        Socket client = mServerSocket.accept();
                        readRequestHeader(client);
                        sendResponse(client);
                        client.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        mDispatcherThread.start();
    }
}