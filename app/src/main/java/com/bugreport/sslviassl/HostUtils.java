package com.bugreport.sslviassl;

/**
 * Simple functions for extracting host and port from an url string
 */
public class HostUtils {
	/**
	 * Extract the host from an url
	 * @param url
	 * @return
     */
	public static String getHostFromUrl(String url){
		url = url.replace("http://", "");
		url = url.replace("https://", "");

		int slashPos = url.indexOf("/");
		if (slashPos != -1) {
			url = url.substring(0, slashPos);
		}

		int colonPos = url.indexOf(":");
		if (colonPos != -1) {
			url = url.substring(0, colonPos);
		}

		return url;
	}

	/**
	 * Extract the port from an url. If the url did not contain any port, return default ports
	 * based on scheme.
	 * @param url
	 * @return
     */
	public static int getPortFromUrl(String url){
		boolean isHttps = url.startsWith("https");
		url = url.replace("http://", "");
		url = url.replace("https://", "");

		int slashPos = url.indexOf("/");
		if (slashPos != -1) {
			url = url.substring(0, slashPos);
		}

		int colonPos = url.indexOf(":");
		if (colonPos != -1) {
			String portString = url.substring(colonPos + 1);
			return Integer.parseInt(portString, 10);
		} else {
			if (isHttps){
				return 443;
			} else {
				return 80;
			}
		}
	}
}
