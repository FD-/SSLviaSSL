# SSLviaSSL
This project demonstrates that creating an SSLSocket over an existing SSLSocket does not work when using the Conscrypt SSL provider.

This program tries to send an HTTP request to an HTTPS server via a Secure Web Proxy (HTTP proxy over SSL/TLS).

# Using the project
Follow these steps to use the project (Project is thought to be used with IntelliJ idea, but general steps work with any IDE):

1. Set up the project:
    - Add the Conscrypt project to the project dependencies
2. Set up a [Secure Web Proxy][1]
    - The easiest way is to run the Server included in server/Server.java:
    - Create a keystore file: keytool -genkey -keystore keystore -keyalg RSA
    - Set KEYSTORE_PATH and KEYSTORE_KEY in server/KeystoreHelper.java      
    - In IntelliJ IDEA, just right-click on Server.java and click "Run 'Server.main()'".    
    - Alternatively, you can set up a Secure Web Proxy using 3rd-party programs by following [the steps below](#how-to-set-up-a-secure-web-proxy-using-3rd-party-programs).
3. Modify PROXY_HOST and PROXY_PORT in Main.java to match the values of your Secure Web Proxy. 
    - These values will be passed to the SecureWebProxyThread.
4. Start the Main program
    - Start by right-click on Main.java and clicking "Run 'Main.main()'". 
    - From the Console, you'll see the second handshake never finishes (Handshake finished is not printed for the second handshake)
5. Test with the JRE-default SSL Provider
    - Modify SecureWebProcyThread#doSSLHandshake() to use the default SSL Provider
    - You'll notice the same code that doesn't work with Conscrypt works with the default SSL Provider

# The issue
When running the program using the Conscrypt SSL Provder and trying to fetch data from an HTTPS server via the Secure Web Proxy (code in SecureWebProxyThread.java), the second handshake (the one between the program and the HTTPS server) never finishes. It seems the call to startHandshake() just never returns.
        
This issues do not occur if the same code is run using the desktop JRE default SSL provider (which you can try by changing the corresponding line in SecureWebProxyThread.java), when no proxy is used, or when only an HTTP server (not HTTPS) is used. This clearly indicates there must be an issue with the second handshake (running an SSLSocket over an existing SSLSocket) in Conscrypt.

Interestingly, on Android versions using the Conscrypt Provider, running an SSLSocket over another SSLSocket does not work either, but an exception is raised. Details on the behaviour on Android can be found in the master branch of this repository.

#  How to set up a [Secure Web Proxy][1]:
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


