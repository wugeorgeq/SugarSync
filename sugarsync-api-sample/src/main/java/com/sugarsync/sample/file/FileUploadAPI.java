package com.sugarsync.sample.file;

import java.io.File;
import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

import com.sugarsync.sample.util.HttpResponse;

/**
 * Sample class for uploading a file
 */
public class FileUploadAPI {
    
    /**
     * The User-Agent HTTP Request header's value 
     */
    private static final String API_SAMPLE_USER_AGENT = "SugarSync API Sample/1.0";

    /**
     * Uploads a local file to the fileDataUrl (Make a HTTP PUT request to
     * fileDataUrl )
     * 
     * @param fileDataUrl
     *            the SugarSync remote file data URL
     * @param localFilePath
     *            the local file path
     * @param accessToken
     *            the SugarSync access token
     * @return the HTTP response
     * @throws IOException
     *             if any I/O error occurs
     */
    public static HttpResponse uploadFile(String fileDataUrl, String localFilePath, String accessToken)
            throws org.apache.commons.httpclient.HttpException, IOException {
        // make the HTTP PUT request
        HttpClient client = new HttpClient();
        PutMethod put = new PutMethod(fileDataUrl);
        File input = new File(localFilePath);
        RequestEntity entity = new FileRequestEntity(input, "Content-Length: " + input.length());
        put.setRequestEntity(entity);
        put.setRequestHeader("Authorization", accessToken);
        put.setRequestHeader("User-Agent",API_SAMPLE_USER_AGENT);
        client.executeMethod(put);

        // get HTTP response
        Integer statusCode = put.getStatusCode();
        String responseBody = put.getResponseBodyAsString();
        Header[] headers = put.getResponseHeaders();

        return new HttpResponse(statusCode, responseBody, headers);
    }

}
