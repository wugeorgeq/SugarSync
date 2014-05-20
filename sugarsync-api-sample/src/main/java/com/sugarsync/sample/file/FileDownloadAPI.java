package com.sugarsync.sample.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import com.sugarsync.sample.util.HttpResponse;

/**
 * Sample class for file download
 */
public class FileDownloadAPI {
	
	private static final byte[] psd = new byte[] {(byte)0x38, (byte)0x42, (byte)0x50, (byte)0x53};
	private static final byte[] docx = new byte[] {(byte)0x50, (byte)0x4b};
	private static final byte[] jpg = new byte[] {(byte)0xff, (byte)0xd8, (byte)0xff};
	private static final byte[] xlsx = docx;
	private static final byte[] ai = new byte[] {(byte)0x25, (byte)0x50};
	private static final byte[] pptx = docx;
	private static final byte[] pdf = new byte[] {(byte)0x25, (byte)0x50, (byte)0x44, (byte)0x46};
	private static final byte[] indd = new byte[] {(byte)0x06, (byte)0x06};
	private static final byte[] ppt = new byte[] {(byte)0xd0, (byte)0xcf};
	private static final byte[] doc = ppt;
	private static final byte[] xls = ppt;
	
	private static final HashMap<String, byte[]> extDict;
	static {
		extDict = new HashMap<String, byte[]>();
		extDict.put("psd", psd);
		extDict.put("docx", docx);
		extDict.put("jpg", jpg);
		extDict.put("xlsx", xlsx);
		extDict.put("ai", ai);
		extDict.put("pptx", pptx);
		extDict.put("pdf", pdf);
		extDict.put("indd", indd);
		extDict.put("ppt", ppt);
		extDict.put("doc", doc);
		extDict.put("xls",  xls);
	}
	
    
    /**
     * The User-Agent HTTP Request header's value 
     */
    private static final String API_SAMPLE_USER_AGENT = "SugarSync API Sample/1.0";
    
    /**
     * Downloads a remote file to the localDownloadPath
     * 
     * @param fileDataURL
     *            the remote file data link
     * @param localDownloadPath
     *            the local path where the file will be downloaded
     * @param accessToken
     *            the SugarSync access token
     * @return the HTTP response
     * @throws IOException
     *             if any I/O error occurs
     */
    public static HttpResponse downloadFileData(String fileDataURL, String localDownloadPath, String accessToken)
            throws IOException {
        // makes the HTTP get request
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(fileDataURL);
        get.setRequestHeader("Authorization", accessToken);
        get.setRequestHeader("User-Agent",API_SAMPLE_USER_AGENT);
        client.executeMethod(get);

        // get the input stream of the response and write its content to the
        // local file
        InputStream in = get.getResponseBodyAsStream();
        FileOutputStream out = new FileOutputStream(new File(localDownloadPath));
        byte[] b = new byte[1024];
        int len = 0;
        while ((len = in.read(b)) != -1) {
            out.write(b, 0, len);
        }
        in.close();
        out.close();

        // get HTTP response
        Integer statusCode = get.getStatusCode();
        Header[] headers = get.getResponseHeaders();

        return new HttpResponse(statusCode, null, headers);
    }
    
    public static boolean downloadFileDataCheckCorrupt(String fileName, String fileDataURL, String accessToken) 
    		throws IOException {
    	// makes the HTTP get request
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(fileDataURL);
        get.setRequestHeader("Authorization", accessToken);
        get.setRequestHeader("User-Agent",API_SAMPLE_USER_AGENT);
        client.executeMethod(get);

        // get the input stream of the response and write to byte array the first # bytes
        InputStream in = get.getResponseBodyAsStream();
        
        String[] splitted = fileName.split("\\.");
        String fileExt = splitted[splitted.length-1].toLowerCase();
        byte[] correct = extDict.get(fileExt);
        boolean isCorrupt = true;
        
        if (correct != null) {
	        // compare these bytes to dictionary of bytes to detect for encryption
	        byte[] b = new byte[correct.length];
	        in.read(b);
	        for (int i = 0; i < correct.length; i++) {
	        	if (correct[i] == b[i]) {
	        		isCorrupt = false;
	        	} else {
	        		isCorrupt = true;
	        		break;
	        	}
	        }
        } else {
        	System.out.println("Extension: " + fileExt + " not found in dictionary");
        }
        in.close();
        return isCorrupt;
    }

}
