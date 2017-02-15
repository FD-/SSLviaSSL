package com.bugreport.sslviassl.server;

import java.io.File;

/**
 * Helper class to ensure a keystore is set. The keystore is required for the SSLServerSocket
 * created by SecureTunnel.java
 */
public class KeystoreHelper {
    private static final String KEYSTORE_PATH = "/set/this/path";
    private static final String KEYSTORE_KEY = "password";

    public static void initKeyStore(){
        File keystoreFile = new File(KEYSTORE_PATH);
        if (!keystoreFile.exists()){
            System.out.println("You have to create a keystore:");
            System.out.println("1. Run 'keytool -genkey -keystore keystore -keyalg RSA'");
            System.out.println("2. Enter data for keystore");
            System.out.println("3. Set KEYSTORE_PATH and KEYSTORE_KEY in KeystoreHelper.java");

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            throw new RuntimeException("No keystore found");
        }

        System.setProperty("javax.net.ssl.keyStore", KEYSTORE_PATH);
        System.setProperty("javax.net.ssl.keyStorePassword", KEYSTORE_KEY);
    }
}
