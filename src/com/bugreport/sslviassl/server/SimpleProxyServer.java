package com.bugreport.sslviassl.server;

import com.bugreport.sslviassl.LineReader;

import java.io.*;
import java.net.*;

/**
 * Very simple HTTP Proxy Server
 */
public class SimpleProxyServer {
    private InetSocketAddress mAddress;
    private ServerSocket mServerSocket = null;
    private Thread mDispatcherThread;

    public SimpleProxyServer(InetSocketAddress address){
        mAddress = address;
    }

    public InetSocketAddress getAddress(){
        if (mServerSocket == null) return null;
        return ((InetSocketAddress) mServerSocket.getLocalSocketAddress());
    }

    public void start(){
        mDispatcherThread = new Thread(){
            @Override
            public void run() {
                try {
                    mServerSocket = new ServerSocket();
                    mServerSocket.bind(mAddress);

                    while (!isInterrupted()){
                        new ProxyThread(mServerSocket.accept()).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        mDispatcherThread.start();
    }

    private static class ProxyThread extends Thread {
        private Socket mClient = null;

        private ProxyThread(Socket socket) {
            mClient = socket;
        }

        public void run() {
            try {
                String destinationString = readRequestHeader();
                if (destinationString == null || !destinationString.contains(":")) {
                    System.out.println("Invalid request: " + destinationString);
                    return;
                }

                String host = destinationString.split(":")[0];
                int port = Integer.parseInt(destinationString.split(":")[1]);
                sendForwardSuccess(mClient);
                startForwarding(host, port);
            } catch (Exception e){
                System.out.println("Exception in proxy thread: " + e);
            }
        }

        private String readRequestHeader() throws IOException {
            LineReader reader = new LineReader(mClient.getInputStream());
            String request = null;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("CONNECT") && request == null){
                    request = line.split(" ")[1];
                }

                if (line.length() == 0) {
                    return request;
                }
            }

            return null;
        }

        private void sendForwardSuccess(Socket socket) throws IOException {
            String response = "HTTP/1.1 200 OK\r\n\r\n";
            socket.getOutputStream().write(response.getBytes());
            socket.getOutputStream().flush();
        }

        private void startForwarding(String destinationHost, int destinationPort) throws IOException {
            Socket destination = new Socket(destinationHost, destinationPort);
            new ForwardThread(destination.getInputStream(), mClient.getOutputStream()).start();
            new ForwardThread(mClient.getInputStream(), destination.getOutputStream()).start();
        }
    }
}
