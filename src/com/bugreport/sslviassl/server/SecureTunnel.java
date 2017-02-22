package com.bugreport.sslviassl.server;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Very simple Java equivalent to the stunnel tool. Wraps a TCP socket with an SSL/TLS layer.
 */
public class SecureTunnel {
    private static final int TIMEOUT = 1000;

    private int mPort;
    private InetSocketAddress mDestinationAddress;
    private Thread mDispatcherThread;
    private SSLServerSocket mServerSocket;

    public SecureTunnel(int port, InetSocketAddress destinationAddress){
        mPort = port;
        mDestinationAddress = destinationAddress;
    }

    public InetSocketAddress getAddress(){
        if (mServerSocket == null) return null;
        return ((InetSocketAddress) mServerSocket.getLocalSocketAddress());
    }

    public void start(){
        if (mDispatcherThread != null) return;

        mDispatcherThread = new Thread(){
            @Override
            public void run() {
                try {
                    SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
                    mServerSocket = (SSLServerSocket) factory.createServerSocket(mPort);

                    while (!isInterrupted()){
                        try {
                            Socket client = mServerSocket.accept();
                            client.setSoTimeout(TIMEOUT);
                            Socket server = new Socket(mDestinationAddress.getAddress(), mDestinationAddress.getPort());
                            server.setSoTimeout(TIMEOUT);
                            new ForwardThread(server.getInputStream(), client.getOutputStream()).start();
                            new ForwardThread(client.getInputStream(), server.getOutputStream()).start();
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        mDispatcherThread.start();
    }
}
