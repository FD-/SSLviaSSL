# SSLviaSSL
This project demonstrates that creating an SSLSocket over an existing SSLSocket does not work on Android.

The app tries to send an HTTP request to an HTTPS server via a Secure Web Proxy.

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
        Location: http://www.google.at/?gfe_rd=cr&ei=4aSgWJ2yHPPM8geUlIz4DQ
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
        <A HREF="http://www.google.at/?gfe_rd=cr&amp;ei=4aSgWJ2yHPPM8geUlIz4DQ">here</A>.
        </BODY></HTML>
        

[1]: https://www.chromium.org/developers/design-documents/secure-web-proxy


