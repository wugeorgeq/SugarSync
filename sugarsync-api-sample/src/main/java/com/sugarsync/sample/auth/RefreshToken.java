package com.sugarsync.sample.auth;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.sugarsync.sample.util.HttpResponse;

/**
 * Sample code used for getting refresh tokens
 * 
 */
public class RefreshToken {
    /**
     * SugarSync App Authorization API url
     */
    private static final String APP_AUTH_REFRESH_TOKEN_API_URL = "https://api.sugarsync.com/app-authorization";
    
    /**
     * The User-Agent HTTP Request header's value 
     */
    private static final String API_SAMPLE_USER_AGENT = "SugarSync API Sample/1.1";
    
    /**
     * The template used for creating the request
     */
    private static final String APP_AUTH_REQUEST_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                                                        + "<appAuthorization>"
                                                            + "<username>%s</username>"
                                                            + "<password>%s</password>"
                                                            + "<application>%s</application>"
                                                            + "<accessKeyId>%s</accessKeyId>" 
                                                            + "<privateAccessKey>%s</privateAccessKey>" 
                                                        + "</appAuthorization>";

    /**
     * Fills the APP_AUTH_REQUEST_TEMPLATE with the request details: replaces
     * "%s" components from the template with request details.
     * 
     * @param username
     *            SugarSync username (email address)
     * @param password
     *            SugarSync password
     * @param application
     *            The app id of a previously created app
     * @param accessKey
     *            Developer application accessKey
     * @param privateAccessKey
     *            Developer application privateAccessKey
     * @return A xml request used as POST data for getting the authorization
     *         token
     */
    private static String fillRequestTemplate(String username, String password, String application,
            String accessKey, String privateAccessKey) {
        return String.format(APP_AUTH_REQUEST_TEMPLATE, new Object[] { username, password, application,
                accessKey, privateAccessKey });
    }

    /**
     * Makes a HTTP POST request to SugarSync app authorization API and returns
     * a HttpResponse.
     * 
     * The refresh token can be retrieved from the HTTP response's header
     * "Location"
     * 
     * 
     * @param username
     *            SugarSync username (email address)
     * @param password
     *            SugarSync password
     * @param application
     *            The app id of a previously created app
     * @param accessKey
     *            Developer accessKey
     * @param privateAccessKey
     *            Developer privateAccessKey
     * @return a HTTP response that contains the refresh token string in the
     *         "Location" response header if the http response status code is
     *         201
     * @throws IOException
     *             if any I/O error if thrown
     */
    public static HttpResponse getAuthorizationResponse(String username, String password, String application,
            String accessKey, String privateAccessKey) throws IOException {
        // fill the request xml template with developer details
        String request = fillRequestTemplate(username, password, application, accessKey, privateAccessKey);

        // make a HTTP POST to APP_AUTH_REFRESH_TOKEN_API_URL with the app authorization request
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(APP_AUTH_REFRESH_TOKEN_API_URL);
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
