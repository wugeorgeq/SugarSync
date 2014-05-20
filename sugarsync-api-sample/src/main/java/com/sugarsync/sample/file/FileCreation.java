package com.sugarsync.sample.file;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.sugarsync.sample.util.HttpResponse;

/**
 * Sample class used for creating a file representation. Note that the file data
 * must be uploaded using a different api
 */
public class FileCreation {
    
    /**
     * The User-Agent HTTP Request header's value 
     */
    private static final String API_SAMPLE_USER_AGENT = "SugarSync API Sample/1.0"; 
    
    /**
     * The template used for creating the file representation
     */

    private static final String CREATE_FILE_REQUEST_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                                                                + "<file>" 
                                                                        + "<displayName>%s</displayName>"
                                                                        + "<mediaType>%s</mediaType>" 
                                                                + "</file>";

    /**
     * Creates a file representation in the folder specified by folderURL
     * parameter.
     * 
     * @param folderURL
     *            the folder resource url
     * @param displayName
     *            the display name of the new file representation
     * @param mediaType
     *            the media type of the new file representation
     * @param accessToken
     *            the SugarSync access token
     * @return the HTTP response
     */
    public static HttpResponse createFile(String folderURL, String displayName, String mediaType,
            String accessToken) throws IOException {
        // fill the request template with file representation details
        String request = fillRequest(displayName, mediaType);
        // make a HTTP POST to the folderURL where the file representation will
        // be created
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(folderURL);
        RequestEntity entity = new StringRequestEntity(request, "application/xml", "UTF-8");
        post.setRequestHeader("Authorization", accessToken);
        post.setRequestHeader("User-Agent",API_SAMPLE_USER_AGENT);
        post.setRequestEntity(entity);
        client.executeMethod(post);

        // get HTTP response
        Integer statusCode = post.getStatusCode();
        String responseBody = post.getResponseBodyAsString();
        Header[] headers = post.getResponseHeaders();

        return new HttpResponse(statusCode, responseBody, headers);
    }

    /**
     * Fills the request template with the file representation details
     * 
     * @param displayName
     *            the file display name
     * @param mediaType
     *            the media type of the file
     * @return A xml request for creating the file representation
     */
    private static String fillRequest(String displayName, String mediaType) {
        return String.format(CREATE_FILE_REQUEST_TEMPLATE, new Object[] { displayName, mediaType });
    }
}
