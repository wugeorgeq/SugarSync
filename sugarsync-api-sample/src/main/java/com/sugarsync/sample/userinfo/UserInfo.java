package com.sugarsync.sample.userinfo;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import com.sugarsync.sample.util.HttpResponse;

/**
 * 
 * Sample class used for getting user information
 * 
 */
public class UserInfo {
    /**
     * SugarSync User info API URL
     */
    private static final String USER_INFO_API_URL = "https://api.sugarsync.com/user";

    /**
     * The User-Agent HTTP Request header's value 
     */
    private static final String API_SAMPLE_USER_AGENT = "SugarSync API Sample/1.0";
    
    /**
     * Returns a UserInfo java bean containing all user information
     * 
     * @param accessToken
     *            the access token
     * @return a HttpResponse instance representing the response for the get
     *         request
     * @throws IOException
     *             if any I/O errors are thrown
     */
    public static HttpResponse getUserInfo(String accessToken) throws IOException {
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(USER_INFO_API_URL);
        get.setRequestHeader("Authorization", accessToken);
        get.setRequestHeader("User-Agent",API_SAMPLE_USER_AGENT);
        client.executeMethod(get);

        // get HTTP response
        Integer statusCode = get.getStatusCode();
        String responseBody = get.getResponseBodyAsString();
        Header[] headers = get.getResponseHeaders();

        return new HttpResponse(statusCode, responseBody, headers);
    }

}
