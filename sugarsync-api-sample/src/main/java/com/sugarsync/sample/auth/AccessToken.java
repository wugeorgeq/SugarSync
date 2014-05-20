package com.sugarsync.sample.auth;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.sugarsync.sample.util.HttpResponse;

/**
 * Sample code used for getting access tokens
 */
public class AccessToken {
    /**
     * SugarSync Access Token API url
     */
    private static final String AUTH_ACCESS_TOKEN_API_URL = "https://api.sugarsync.com/authorization";

    /**
     * The User-Agent HTTP Request header's value
     */
    private static final String API_SAMPLE_USER_AGENT = "SugarSync API Sample/1.0";

    /**
     * The template used for creating the request
     */
    private static final String ACCESS_TOKEN_AUTH_REQUEST_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                                                                    + "<tokenAuthRequest>"
                                                                            + "<accessKeyId>%s</accessKeyId>"
                                                                            + "<privateAccessKey>%s</privateAccessKey>"
                                                                            + "<refreshToken>%s</refreshToken>"
                                                                    + "</tokenAuthRequest>";

    /**
     * Fills the ACCESS_TOKEN_AUTH_REQUEST_TEMPLATE with the request details:
     * replaces "%s" components from the template with request details.
     * 
     * @param accessKey
     *            Developer application accessKey
     * @param privateAccessKey
     *            Developer application privateAccessKey
     * @param refreshToken
     *            Refresh token obtained from appAuthorization end-point (see
     *            {@link RefreshToken} )
     * @return A xml request used as POST data for getting the access token
     */
    private static String fillRequestTemplate(String accessKey, String privateAccessKey, String refreshToken) {
        return String.format(ACCESS_TOKEN_AUTH_REQUEST_TEMPLATE, new Object[] { accessKey, privateAccessKey,
                refreshToken });
    }

    /**
     * Makes a HTTP POST request to SugarSync authorization API and returns a
     * HttpResponse.
     * 
     * The access token can be retrieved from the HTTP response's header
     * "Location"
     * 
     * 
     * @param username
     *            SugarSync username (email address)
     * @param password
     *            SugarSync password
     * @param application
     *            The developer application id
     * @param accessKey
     *            Developer accessKey
     * @param privateAccessKey
     *            Developer privateAccessKey
     * @return a HTTPResponse that contains the access token in the "Location"
     *         response header if the HTTP response status code is 201
     * @throws IOException
     *             if any I/O error if thrown
     */
    public static HttpResponse getAccessTokenResponse(String accessKey, String privateAccessKey,
            String refreshToken) throws IOException {
        // fill the request xml template with developer details
        String request = fillRequestTemplate(accessKey, privateAccessKey, refreshToken);

        // make a HTTP POST to AUTH_ACCESS_TOKEN_API_URL with the authorization
        // request
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(AUTH_ACCESS_TOKEN_API_URL);
        RequestEntity entity = new StringRequestEntity(request, "application/xml", "UTF-8");
        post.setRequestEntity(entity);
        post.setRequestHeader("User-Agent", API_SAMPLE_USER_AGENT);
        client.executeMethod(post);

        // get HTTP response
        Integer statusCode = post.getStatusCode();
        String responseBody = post.getResponseBodyAsString();
        Header[] headers = post.getResponseHeaders();

        return new HttpResponse(statusCode, responseBody, headers);
    }
}
