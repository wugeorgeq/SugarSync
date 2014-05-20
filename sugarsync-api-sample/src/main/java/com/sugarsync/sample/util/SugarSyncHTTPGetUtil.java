package com.sugarsync.sample.util;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Sample class used for making HTTP GET requests
 */
public class SugarSyncHTTPGetUtil {
    /**
     * The User-Agent HTTP Request header's value 
     */
    private static final String API_SAMPLE_USER_AGENT = "SugarSync API Sample/1.0";

    /**
     * Makes a HTTP GET request to the url
     * 
     * 
     * @param url
     *            the url where the GET request will be made
     * @param authToken
     *            the SugarSync authorization token
     * @return the server HTTP response
     * @throws IOException
     *             if any I/O error occurs
     */
    public static HttpResponse getRequest(String url, String authToken) throws IOException {
        // make the HTTP GET request
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(url);
        get.setRequestHeader("Authorization", authToken);
        get.setRequestHeader("User-Agent",API_SAMPLE_USER_AGENT);
        client.executeMethod(get);

        // get HTTP response
        Integer statusCode = get.getStatusCode();
        String responseBody = get.getResponseBodyAsString();
        Header[] headers = get.getResponseHeaders();

        return new HttpResponse(statusCode, responseBody, headers);
    }

}
