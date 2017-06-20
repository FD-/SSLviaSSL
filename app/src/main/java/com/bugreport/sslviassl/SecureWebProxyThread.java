package com.bugreport.sslviassl;

import org.conscrypt.OpenSSLProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Connects to an http or https server via an optional Secure Web Proxy.
 */
class SecureWebProxyThread extends Thread{
    private static final int BUFFER_SIZE = 4096;

    interface ProxyThreadResultListener{
        void onResultReceived(String result);
        void onException(Exception exception);
    }

    private ProxyThreadResultListener mListener;

    private String mProxyHost;
    private int mProxyPort;
    private String mDestinationUrl;

    /**
     * Create a new SecureWebProxyThread with a given proxyHost, proxyPort and destinationUrl.
     * If proxyHost is null, the connection will be made to the destination host directly, withou
     * using any proxy
     * @param proxyHost The proxy host. If null or empty, the connection will be made to the destination host directly, without
     * using any proxy
     * @param proxyPort The proxy port
     * @param destinationUrl The url to fetch from the destination server. Destination host and port will be extracted from the url.
     */
    SecureWebProxyThread(String proxyHost, int proxyPort, String destinationUrl){
        mProxyHost = proxyHost;
        mProxyPort = proxyPort;
        mDestinationUrl = destinationUrl;
    }

    void setListener(ProxyThreadResultListener listener){
        mListener = listener;
    }

    /**
     * Send an HTTP CONNECT request to the proxy and read the response from the proxy.
     * @param proxy
     * @param host
     * @param port
     * @throws IOException
     */
    private void connectToHostViaSecureWebProxy(Socket proxy, String host, int port) throws IOException {
        String request = "CONNECT " + host + ":" + port + " HTTP/1.0\r\n\r\n";
        proxy.getOutputStream().write(request.getBytes());
        proxy.getOutputStream().flush();

        LineReader reader = new LineReader(proxy.getInputStream());
        String line;
        boolean success = false;
        while ((line = reader.readLine()) != null){
            if (success || line.contains("200")) {
                success = true;
            } else {
                throw new IOException("Could not connect to proxy: " + line);
            }

            if (line.length() == 0) return;
        }
    }

    /**
     * Connect to the destination host directly (without a proxy). Used when proxyHost is null or
     * empty.
     * @param host
     * @param port
     * @return
     * @throws IOException
     */
    private Socket connectToDestinationHostDirectly(String host, int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port));
        return socket;
    }

    /**
     * Create a Socket to the specified Secure Web Proxy and use it to connect to the specificed
     * destination host.
     * @param proxyHost
     * @param proxyPort
     * @param destinationHost
     * @param destinationPort
     * @return
     * @throws IOException
     */
    private Socket connectToDestinationHostViaSecureWebProxy(String proxyHost, int proxyPort, String destinationHost, int destinationPort) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(proxyHost, proxyPort));
        socket = doSSLHandshake(socket, proxyHost, proxyPort);

        connectToHostViaSecureWebProxy(socket, destinationHost, destinationPort);

        return socket;
    }

    /**
     * Create an SSLSocket over the socket and do a handshake with the destination host described by
     * host and port.
     * <br><b>NOTE:</b> On Android, running this function using an SSLSocket as socket leads to an
     * exception during the handshake.
     * @param socket The socket to run the SSLSocket over
     * @param host The host at the other end of the socket
     * @param port The port at the other end of the socket
     * @return The resulting SSLSocket
     * @throws IOException
     */
    private Socket doSSLHandshake(Socket socket, String host, int port) throws IOException {
        // For easier debugging purpose, trust all certificates
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager(){
                    public X509Certificate[] getAcceptedIssuers(){ return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        };

        System.out.println("Doing SSL handshake with " + host + ":" + port);

        try {
            // Uncomment to make Conscrypt use the engine-based sockets
            // Conscrypt.SocketFactories.setUseEngineSocketByDefault(true);

            Provider provider = new OpenSSLProvider(); // Use Conscrypt provider

            SSLContext sslContext;
            if (provider == null) {
                sslContext = SSLContext.getInstance("TLS");
            } else {
                sslContext = SSLContext.getInstance("TLS", provider);
            }
            
            sslContext.init(null, trustAllCerts, new SecureRandom());
            SSLSocketFactory factory = sslContext.getSocketFactory();
            
            SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, host, port, false);
            sslSocket.setEnabledProtocols(new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"});
            System.out.println("Enabled protocols are: " + Arrays.toString(sslSocket.getEnabledProtocols()));

            sslSocket.startHandshake();

            System.out.println("Handshake finished");

            return sslSocket;
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new IOException("Could not do SSL handshake: " + e);
        }
    }

    private void sendHttpRequest(Socket socket, String host, String path) throws IOException {
        String request = "GET " + path + " HTTP/1.1\r\n" +
                "Host: " + host + "\r\n" +
                "Connection: close\r\n\r\n";
        socket.getOutputStream().write(request.getBytes());
        socket.getOutputStream().flush();
    }

    /**
     * Just a simple function to skip the HTTP response header so the next function
     * can read the actual response data
     * @param socket
     * @throws IOException
     */
    private void readResponseHeader(Socket socket) throws IOException {
        LineReader reader = new LineReader(socket.getInputStream());

        String line;
        while ((line = reader.readLine()) != null){
            if (line.length() == 0) {
                return;
            }
        }
    }

    /**
     * Read the HTTP response data. As we're sending Connection: close in the request,
     * we can simply read until the end of the stream here.
     * @param socket
     * @return
     * @throws IOException
     */
    private String readResponseData(Socket socket) throws IOException {
        InputStream stream = socket.getInputStream();

        BufferedReader in = new BufferedReader(new InputStreamReader(stream), BUFFER_SIZE);
        StringBuilder responseData = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            responseData.append(line).append('\n');
        }

        return responseData.toString();
    }

    @Override
    public void run() {
        Socket socket = null;

        try {
            String destinationHost = HostUtils.getHostFromUrl(mDestinationUrl);
            int destinationPort = HostUtils.getPortFromUrl(mDestinationUrl);
            String path = mDestinationUrl.replace("https://", "").replace("http://", "").replace(destinationHost, "").replace(":" + destinationPort, "");
            if (path.length() == 0) path = "/";

            if (mProxyHost == null || mProxyHost.length() == 0){
                socket = connectToDestinationHostDirectly(destinationHost, destinationPort);
            } else {
                socket = connectToDestinationHostViaSecureWebProxy(mProxyHost, mProxyPort, destinationHost, destinationPort);
            }

            if (mDestinationUrl.startsWith("https")){
                socket = doSSLHandshake(socket, destinationHost, destinationPort);
            }

            sendHttpRequest(socket, destinationHost, path);
            readResponseHeader(socket);
            String data = readResponseData(socket);

            if (mListener != null){
                mListener.onResultReceived(data);
            }
        } catch (Exception e){
            e.printStackTrace();
            if (mListener != null){
                mListener.onException(e);
            }
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (Exception ignored){}
        }
    }
}
