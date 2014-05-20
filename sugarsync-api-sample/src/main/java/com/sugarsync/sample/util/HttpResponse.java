package com.sugarsync.sample.util;

import org.apache.commons.httpclient.Header;

/**
 * Java bean class used for a HTTP response
 */
public class HttpResponse {
    private Integer httpStatusCode;
    private String responseBody;
    private Header[] headers;

    /**
     * 
     * @param httpStatusCode
     *            The HTTP response status code
     * @param responseBody
     *            The response body
     * @param headers
     *            A Map with the response headers
     */
    public HttpResponse(Integer httpStatusCode, String responseBody, Header[] headers) {
        this.httpStatusCode = httpStatusCode;
        this.responseBody = responseBody;
        this.headers = headers;
    }

    /**
     * 
     * @return the HTTP response status code
     */
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * @return the HTTP response body
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * @return the HTTP response headers
     */
    public Header[] getHeaders() {
        return headers;
    }

    public Header getHeader(String headerName) {
        for (Header header : headers) {
            if (header.getName().equals(headerName)) {
                return header;
            }
        }
        return null;
    }
}
