package com.bugreport.sslviassl.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Runs a simplified Secure Web Proxy.
 * Can be used as the server for the Android application (MainActivity.java) or the JRE equivalent
 * (Main.java).
 * If you are in Android-Studio, just right-click on the file in the Project view and
 * click "Run 'Server.main()'". This will execute the program in the desktop JRE.
 */
public class Server {
    public static void main(String[] args) {
        KeystoreHelper.initKeyStore();

        SimpleProxyServer proxyServer = new SimpleProxyServer(new InetSocketAddress("127.0.0.1", 8080));
        proxyServer.start();

        System.out.println("Starting Proxy Server...");

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Proxy Server is running at: " + proxyServer.getAddress());
        System.out.println("Starting Secure Web Proxy...");

        SecureTunnel proxyTunnel = new SecureTunnel(10443, proxyServer.getAddress());
        proxyTunnel.start();

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Secure Proxy Server is running at: " + proxyTunnel.getAddress());

        System.out.println("\n\n\nMachine can be reached on interfaces:");
        try {
            listInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    // Code taken from http://stackoverflow.com/a/11721253/1691231
    private static void listInterfaces() throws SocketException {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets))
            displayInterfaceInformation(netint);
    }

    private static void displayInterfaceInformation(NetworkInterface netint) {
        System.out.printf("Display name: %s\n", netint.getDisplayName());
        System.out.printf("Name: %s\n", netint.getName());
        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            System.out.printf("InetAddress: %s\n", inetAddress);
        }
        System.out.printf("\n");
    }
}
