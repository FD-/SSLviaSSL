# SSLviaSSL
This project demonstrates that creating an SSLSocket over an existing SSLSocket does not work on Android.

The app tries to send an HTTP request to an HTTPS server via a Secure Web Proxy (HTTP proxy over SSL/TLS).

# Using the project
Follow these steps to use the project:

1. Set up a [Secure Web Proxy][1]
    - The easiest way is to run the Server included in server/Server.java:
    - Create a keystore file: keytool -genkey -keystore keystore -keyalg RSA
    - Set KEYSTORE_PATH and KEYSTORE_KEY in server/KeystoreHelper.java      
    - In Android Studio, just right-click on Server.java and click "Run 'Server.main()'".    
    - Alternatively, you can set up a Secure Web Proxy using 3rd-party programs by following [the steps below](#how-to-set-up-a-secure-web-proxy-using-3rd-party-programs).
2. Modify PROXY_HOST and PROXY_PORT in Main.java to match the values of your Secure Web Proxy. 
    - These values will be used when running the JRE program and will be used as defaults for the Android app.
3. Start the Android app
    - The exception that occurs at the second handshake will be shown in the app and printed to Logcat.
4. Start the JRE program
    - The Main.java class can be run in the desktop JRE directly from within Android Studio. Just right-click the file and click "Run 'Main.main()'". This will execute the program in the desktop JRE and print the output within Android Studio's console.
    - You'll notice the same code that doesn't work on Android works in the desktop JRE.
 

# The issue
When running the app on Android and trying to fetch data from an HTTPS server via the Secure Web Proxy (code in SecureWebProxyThread.java), the second handshake (the one between the Android app and the HTTPS server) fails with this exception:

    javax.net.ssl.SSLHandshakeException: Handshake failed
        at com.android.org.conscrypt.OpenSSLSocketImpl.startHandshake(OpenSSLSocketImpl.java:429)
        at com.bugreport.sslviassl.SecureWebProxyThread.doSSLHandshake(SecureWebProxyThread.java:147)
        at com.bugreport.sslviassl.SecureWebProxyThread.run(SecureWebProxyThread.java:216)
    Caused by: javax.net.ssl.SSLProtocolException: SSL handshake aborted: ssl=0x74621f1a40: Failure in SSL library, usually a protocol error
    error:100000e3:SSL routines:OPENSSL_internal:UNKNOWN_ALERT_TYPE (external/boringssl/src/ssl/s3_pkt.c:618 0x74705b3e7e:0x00000000)
        at com.android.org.conscrypt.NativeCrypto.SSL_do_handshake(Native Method)
        at com.android.org.conscrypt.OpenSSLSocketImpl.startHandshake(OpenSSLSocketImpl.java:357)
    	... 2 more        
        
This exception does not happen if the same code is run in a desktop JRE (which you can try by running Main.java), when no proxy is used, or when only an HTTP server (not HTTPS) is used. This clearly indicates there must be an issue with the second handshake (running an SSLSocket over an existing SSLSocket) on Android.

__Note:__ It seems like the specific error message depends on the Android version used (as they use different Security providers). The exception above occurs when using Android 7.1.1.

# TCPDUMP
I added tcpdumps of two runs to the `tcpdumps` directory. Dumps were taken with `sudo tcpdump -i any -s 0 -w file_name.tcpdump`;
- android_ssl_via_ssl.tcpdump:
      - Taken from virtual machine hosting the proxy server and https server. 
      - Proxy server was squid3, running on port 8080, secured via an stunnel (stunnel4) on port 10443. 
      - HTTPS server was apache2, running on port 80 (HTTP) and 443 (HTTPS)
      - Server machine had IP 10.211.55.13
      - Android device had IP 10.211.55.2
- jre_ssl_via_ssl.tcpdump:
      - Taken from virtual machine hosting the proxy server and https server. 
      - Proxy server was squid3, running on port 8080, secured via an stunnel (stunnel4) on port 10443. 
      - HTTPS server was apache2, running on port 80 (HTTP) and 443 (HTTPS)
      - Server machine had IP 10.211.55.13
      - Client machine running JRE had IP 10.211.55.2      

#  How to set up a [Secure Web Proxy][1] using 3rd-party programs:
These steps were tested on a vanilla Ubuntu 14.04 image.

(Alternatively, Squid can be run via SSL without any external tool, but it has to be compiled specifically for that: http://wiki.squid-cache.org/Features/HTTPS)

1. Install Squid:

        sudo apt-get install squid3

2. Edit the Squid config file at /etc/squid3/squid.conf:
(If the file doesn’t exist, you’ll have to create it)

        sudo nano /etc/squid3/squid.conf

    Use a config file like this:

        http_port 8080
        cache_dir ufs /var/spool/squid3 100 16 256
        cache_mgr local@localhost
        access_log /var/log/squid3/access.log combined
        http_access allow all

3. (re)Start Squid to apply the new config:

        sudo service squid3 restart

4. Install stunnel:

        apt-get install stunnel4 -y

5. Create a certificate:

        openssl genrsa -out key.pem 2048
        openssl req -new -x509 -key key.pem -out cert.pem -days 1095
        cat key.pem cert.pem >> stunnel.pem
        sudo cp stunnel.pem /etc/stunnel/

6. Edit the stunnel config file at /etc/stunnel/stunnel.conf

        sudo nano /etc/stunnel/stunnel.conf

    Use a config file like this:

        client = no
        [squid]
        accept = 10443
        connect = 127.0.0.1:8080
        cert = /etc/stunnel/stunnel.pem 

7. Start an SSL tunnel for the http proxy:

        sudo stunnel4

8. Test if tunnel and proxy work:

        URL="http://google.com" PROXY=127.0.0.1:10443; echo -e "GET $URL HTTP/1.0\\n\\n" | openssl s_client -connect $PROXY -ign_eof

    Should return:
    
        HTTP/1.1 302 Moved Temporarily
        Cache-Control: private
        Content-Type: text/html; charset=UTF-8
        Location: http://www.google.{depends on your location}
        Content-Length: 258
        Date: Sun, 12 Feb 2017 18:09:37 GMT
        X-Cache: MISS from ubuntu
        X-Cache-Lookup: MISS from ubuntu:8080
        Via: 1.1 ubuntu (squid/3.3.8)
        Connection: close
        
        <HTML><HEAD><meta http-equiv="content-type" content="text/html;charset=utf-8">
        <TITLE>302 Moved</TITLE></HEAD><BODY>
        <H1>302 Moved</H1>
        The document has moved
        <A HREF="http://www.google.{depends on your location}">here</A>.
        </BODY></HTML>
        

[1]: https://www.chromium.org/developers/design-documents/secure-web-proxy


