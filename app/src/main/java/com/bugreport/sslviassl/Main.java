package com.bugreport.sslviassl;

/**
 * This class can be used to demonstrate that SSL via SSL using the exact same code does work in a desktop JRE.
 * If you are in Android-Studio, just right-click on the file in the Project view and
 * click "Run 'Main.main()'". This will execute the program in the desktop JRE.
 *
 * The exact same code (SecureWebProxyThread) that fails on Android works in a desktop JRE.
 */
public class Main {
    // Default values for both MainActivity and Main
    public static final String PROXY_HOST = "10.211.55.13";
    public static final int PROXY_PORT = 10443;
    public static final String DESTINATION_URL = "http://10.211.55.13";

    public static void main(String[] args) {
        SecureWebProxyThread thread = new SecureWebProxyThread(PROXY_HOST, PROXY_PORT, DESTINATION_URL);
        SecureWebProxyThread.ProxyThreadResultListener listener = new SecureWebProxyThread.ProxyThreadResultListener() {
            @Override
            public void onResultReceived(String result) {
                System.out.println("Have result: " + result);
            }

            @Override
            public void onException(Exception exception) {
                exception.printStackTrace();
            }
        };
        thread.setListener(listener);
        thread.start();
    }
}
