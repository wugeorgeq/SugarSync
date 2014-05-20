package com.sugarsync.sample.tool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import com.sugarsync.sample.auth.AccessToken;
import com.sugarsync.sample.auth.RefreshToken;
import com.sugarsync.sample.file.FileCreation;
import com.sugarsync.sample.file.FileDownloadAPI;
import com.sugarsync.sample.file.FileUploadAPI;
import com.sugarsync.sample.george.Parser;
import com.sugarsync.sample.userinfo.UserInfo;
import com.sugarsync.sample.util.HttpResponse;
import com.sugarsync.sample.util.SugarSyncHTTPGetUtil;
import com.sugarsync.sample.util.XmlUtil;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * @file SampleTool.java
 * 
 *       The main class for the sample api tool. The tool has the next features:
 * 
 *       - displays quota information for a developer
 * 
 *       - lists the "Magic Briefcase" folder contents
 * 
 *       - downloads a remote file from the "Magic Briefcase" folder
 * 
 *       - uploads a local file to the "Magic Briefcase" folder
 */
public class SampleTool {
    static{
        //set the logging level to error
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "error");
    }
    
    // used for printing user QUOTA
    private static final Double ONE_GB = 1024.0 * 1024 * 1024;

    // tool parameters and commands
    private static final String userParam = "-user";
    private static final String passParam = "-password";
    private static final String applicationIdParam = "-application";
    private static final String accesskeyParam = "-accesskey";
    private static final String privateaccesskeyParam = "-privatekey";
    

    private static final String quotaCmd = "quota";
    private static final String listCmd = "list";
    private static final String uploadCmd = "upload";
    private static final String downloadCmd = "download";
    private static final String getReceivedShareInfo = "getshared";
    
    //George: for use with getting list of files
    private static HttpClient client;
    /**
    private static HashMap<String, ArrayList<String>> directories;
    private static String allFolderNames;
    
    
    
    **/
    private static int globalFileCounter;
    private static HashMap<String, String> fileToVersHistory;
    
    private static ArrayList<ArrayList<String>> allPaths;
    private static ArrayList<String> allFileNames;
    private static ArrayList<String> allFileRefURLs;
    
    private static String globalToken;
   

    /**
     * Returns the value of the parameter value specified as argument
     * 
     * @param param
     *            the parameter for which the value is requested
     * @param argumentList
     *            the arguments passed to main method
     * @return the value of the input parameter
     */
    private static String getParam(String param, List<String> argumentList) {
        int indexOfParam = argumentList.indexOf(param);
        if (indexOfParam == -1) {
            System.out.println("Parameter " + param + " not specified!!!");
            printUsage();
            System.exit(0);
        }
        return argumentList.get(indexOfParam + 1);
    }

    /**
     * Returns the command for the tool
     * 
     * @param argumentList
     *            the arguments passed to main method
     * @return the command which will be run by the tool
     */
    private static String getCommand(List<String> argumentList) {
        String cmd = argumentList.get(argumentList.size() - 1);
        if (Arrays.asList(quotaCmd, listCmd, getReceivedShareInfo).contains(cmd)) {
            return cmd;
        } else
            return argumentList.get(argumentList.size() - 2);
    }

    // --- SugarSync API calls
    /**
     * Returns a refresh token for the developer with the credentials specified
     * in the method parameters and the application id
     * 
     * @param username
     *            SugarSync username (email address)
     * @param password
     *            SugarSync password
     * @param applicationId
     *            The developer application id
     * @param accessKey
     *            Developer accessKey
     * @param privateAccessKey
     *            Developer privateAccessKey
     * @return refresh token
     * @throws IOException
     *             if any I/O error if thrown
     */
    private static String getRefreshToken(String username, String password, String applicationId, String accessKey,
            String privateAccessKey) throws IOException {
        HttpResponse httpResponse = null;
        httpResponse = RefreshToken.getAuthorizationResponse(username, password,applicationId, accessKey, privateAccessKey);

        if (httpResponse.getHttpStatusCode() > 299) {
            System.out.println("Error while getting refresh token!");
            printResponse(httpResponse);
            System.exit(0);
        }

        return httpResponse.getHeader("Location").getValue();
    }
    
    /**
     * Return an access token for the developer keys and a refresh token
     * 
     * @param accessKey
     *            Developer accessKey
     * @param privateAccessKey
     *            Developer privateAccessKey
     * @param refreshToken
     *            Refresh token string returned ass a response from
     *            app-authorization request
     * @return the access token that will be used for all API requests
     * @throws IOException
     *             if any I/O error if thrown
     */
    private static String getAccessToken(String accessKey, String privateAccessKey, String refreshToken) throws IOException {
        HttpResponse httpResponse = AccessToken.getAccessTokenResponse(accessKey, privateAccessKey,
                refreshToken);

        if (httpResponse.getHttpStatusCode() > 299) {
            System.out.println("Error while getting access token!");
            printResponse(httpResponse);
            System.exit(0);
        }
        globalToken = httpResponse.getHeader("Location").getValue();
        return globalToken;
    }

    /**
     * Returns the account information
     * 
     * @param accessToken
     *            the access token
     * @return a HttpResponse containing the server's xml response in the
     *         response body
     * @throws IOException
     *             if any I/O error occurs
     */
    private static HttpResponse getUserInfo(String accessToken) throws IOException {
        HttpResponse httpResponse = UserInfo.getUserInfo(accessToken);
        validateHttpResponse(httpResponse);
        return httpResponse;
    }

    /**
     * Returns the "Magic Briefcase" SugarSync default folder contents
     * 
     * 1.Get the "Magic Briefcase" folder representation
     * 
     * 2.Extract the folder contents link from the folder representation: parse
     * the xml file and retrieve the <contents> node value
     * 
     * 3.Make a HTTP GET to the previous extracted link
     * 
     * @param accessToken
     *            the access token
     * @return a HttpResponse containing the server's xml response in the
     *         response body
     * @throws IOException
     *             if any I/O error occurs
     */
    private static HttpResponse getMagicBriefcaseFolderContents(String accessToken)
            throws IOException, XPathExpressionException {
        HttpResponse folderRepresentationResponse = getMagicBriefcaseFolderRepresentation(accessToken);
        validateHttpResponse(folderRepresentationResponse);

        String magicBriefcaseFolderContentsLink = XmlUtil.getNodeValues(
                folderRepresentationResponse.getResponseBody(), "/folder/contents/text()").get(0);
        HttpResponse folderContentsResponse = SugarSyncHTTPGetUtil.getRequest(
                magicBriefcaseFolderContentsLink, accessToken);
        validateHttpResponse(folderContentsResponse);

        return folderContentsResponse;
    }

    /**
     * Returns the "Magic Briefcase" SugarSync default folder representation.
     * 
     * 1. Make a HTTP GET call to https://api.sugarsync.com/user for the user
     * information
     * 
     * 2. Extract <magicBriefcase> node value from the xml response
     * 
     * 3. Make a HTTP GET to the previous extracted link
     * 
     * @param accessToken
     *            the access token
     * @return a HttpResponse containing the server's xml response in the
     *         response body
     * @throws IOException
     *             if any I/O error occurs
     */
    private static HttpResponse getMagicBriefcaseFolderRepresentation(String accessToken)
            throws IOException, XPathExpressionException {
        HttpResponse userInfoResponse = getUserInfo(accessToken);

        // get the magicBriefcase folder representation link
        String magicBriefcaseFolderLink = XmlUtil.getNodeValues(userInfoResponse.getResponseBody(),
                "/user/magicBriefcase/text()").get(0);

        // make a HTTP GET to the link extracted from user info
        HttpResponse folderRepresentationResponse = SugarSyncHTTPGetUtil.getRequest(magicBriefcaseFolderLink,
                accessToken);
        validateHttpResponse(folderRepresentationResponse);

        return folderRepresentationResponse;
    }

    // --- End SugarSync API calls

    // --- tool commands method
    /**
     * Handles "quota" tool command. Makes a HTTP GET call to
     * https://api.sugarsync.com/user and displays the quota information from
     * the server xml response.
     * 
     * @param accessToken
     *            the access token
     * @throws IOException
     *             if any I/O error occurs
     * @throws XPathExpressionException
     */
    private static void handleQuotaCommand(String accessToken) throws IOException,
            XPathExpressionException {
        HttpResponse httpResponse = getUserInfo(accessToken);
        System.out.println("httpResponse:\n");
        System.out.println(httpResponse.getResponseBody());

        // read the <quota> node values
        String limit = XmlUtil.getNodeValues(httpResponse.getResponseBody(), "/user/quota/limit/text()").get(
                0);
        String usage = XmlUtil.getNodeValues(httpResponse.getResponseBody(), "/user/quota/usage/text()").get(
                0);

        DecimalFormat threeDForm = new DecimalFormat("#.###");
        // print quota info
        String storageAvailableInGB = threeDForm.format(Double.valueOf(limit) / ONE_GB);
        String storageUsageInGB = threeDForm.format(Double.valueOf(usage) / ONE_GB);
        String freeStorageInGB = threeDForm.format(((Double.valueOf(limit) - Double.valueOf(usage)) / ONE_GB));
        System.out.println("\n---QUOTA INFO---");
        System.out.println("Total storage available: " + storageAvailableInGB + " GB");
        System.out.println("Storage usage: " + storageUsageInGB + " GB");
        System.out.println("Free storage: " + freeStorageInGB + " GB");
    }

    /**
     * Handles "list" tool command. Makes a HTTP GET request to the
     * "Magic Briefcase" contents link and displays the file and folder names
     * within the folder
     * 
     * @param accessToken
     *            the access token
     * @throws IOException
     * @throws XPathExpressionException
     * @throws TransformerException
     */
    private static void handleListCommand(String accessToken) throws IOException,
            XPathExpressionException, TransformerException {
        HttpResponse folderContentsResponse = getMagicBriefcaseFolderContents(accessToken);

        printFolderContents(folderContentsResponse.getResponseBody());
    }
    
    /**
     * Hopefully, can get files from the folder, B_Product_Starter_Account
     * 
     * This is the syncfolders I got back from getting my userInfo:
     * <syncfolders>https://api.sugarsync.com/user/8011490/folders/contents</syncfolders>
     * 
     * Using that, should be able to retrieve sync folders collection
     * https://www.sugarsync.com/dev/api/method/get-syncfolders.html
     * 
     * Actually, it's a shared resource so look here:
     * https://www.sugarsync.com/dev/api/method/get-received-shares.html
     * 
     * Retrieves information about retreived share, hopefully leads us to folder/file names
     * 
     * 
     * @param accessToken
     *            the access token
     * @return ReceivedShare Information as xml in String format
     */
    private static String handleGetReceivedShareInfo(String accessToken) throws IOException, 
    		XPathExpressionException, TransformerException {
    	HttpResponse userInfoResponse = getUserInfo(accessToken);
    	//get receivedShares list from userInfo
        String receivedSharesLink = XmlUtil.getNodeValues(userInfoResponse.getResponseBody(),
                "/user/receivedShares/text()").get(0);
        
        /* Implement an HTTP GET method on the resource */
        GetMethod get = new GetMethod(receivedSharesLink);
        /* Create an access token and set it in the request header */
        String token = accessToken;
        get.addRequestHeader("Authorization", token);
        /* Execute the HTTP GET request */
        client.executeMethod(get);
        /* Display the response */
        //System.out.println("Response status code: " + get.getStatusCode());
        //System.out.println("Response body: ");

        byte[] responseBody = get.getResponseBody();
        //System.out.println(new String(responseBody));
        return new String(responseBody);
    }
    
    
    /**
     * We get sharedFolder information from the getRequest for shared information for this user, 
     * Note, that there may be multiple receivedShare 's and sharedFolder 's for this user
     * 
     * Here, we want to get the folder information for B_Product_Starter_Account from one of the receivedShares
     * @param receivedShareInfo
     * 
     * @return String URL of contents of folder, in this case, the B_Product_Starter_Account folder
     * @throws XPathExpressionException 
     * @throws IOException 
     * @throws HttpException 
     */
    private static String getFolderInformation(String receivedShareInfo, String displayName, String accessToken) throws XPathExpressionException, HttpException, IOException {
    	String folderURL = null;
		List<String> sharedFolders = XmlUtil.getNodeValues(receivedShareInfo, "/receivedShares/receivedShare/displayName/text()");
		List<String> sharedFolderURLS = XmlUtil.getNodeValues(receivedShareInfo, "/receivedShares/receivedShare/sharedFolder/text()");
		//System.out.println("list of receivedShares, size: " + sharedFolders.size());
		//System.out.println(sharedFolders.get(0));
		//System.out.println(sharedFolders.get(1));
		//here, we want get(1), the second sharedFolder, as it is B_Product_Starter_Account
		for (int i = 0; i < sharedFolders.size(); i++) {
			if (sharedFolders.get(i).equals(displayName)) {
				folderURL = sharedFolderURLS.get(i);
			}
		}
    	if (folderURL != null) {
			String folderInformation = null;
			//return folderURL;
	        /* Implement an HTTP GET method on the resource */
	        GetMethod get = new GetMethod(folderURL);
	        /* Create an access token and set it in the request header*/
	        get.addRequestHeader("Authorization", accessToken);
	        /* Execute the HTTP GET request */
			client.executeMethod(get);
			/* Display the response */
	        byte[] responseBody = get.getResponseBody();
	        folderInformation = new String(responseBody);
	        //System.out.println(folderInformation);
	        List<String> contents = XmlUtil.getNodeValues(folderInformation, "/folder/contents/text()");
	        return contents.get(0);
    	} else {
    		System.out.println("Error, couldn't find a shared folder matching the name of: " + displayName);
    		return null;
    	}
    }
    
    /**
     * Recursively traverses all folders and calls getStoreFileVersionHistory on all files it encounters that 
     * match with the given list from Tim
     * 
     * @param contentsURL from getFolderInformation
     * @param accessToken
     * @param pathName not needed
     * @throws XPathExpressionException
     * @throws HttpException
     * @throws IOException
     */
    
    /**
    private static void traverseFolder(String contentsURL, String accessToken, String pathName) throws XPathExpressionException, HttpException, IOException {
  		GetMethod get = new GetMethod(contentsURL);
  		get.addRequestHeader("Authorization", accessToken);
  		client.executeMethod(get);
  		byte[] responseBody = get.getResponseBody();
  		String folderContents = new String(responseBody);
  		List<String> subFolderNames = XmlUtil.getNodeValues(folderContents, "/collectionContents/collection[@type='folder']/displayName/text()");
  		List<String> subFolderContents = XmlUtil.getNodeValues(folderContents, "/collectionContents/collection[@type='folder']/contents/text()");
  		List<String> subFileNames = XmlUtil.getNodeValues(folderContents, "/collectionContents/file/displayName/text()");
  		List<String> subFileRefs = XmlUtil.getNodeValues(folderContents, "/collectionContents/file/ref/text()");
  		
  		
		//now we look for the file in all directories
		for (int i = 0; i < subFileNames.size(); i++) {
			if (Parser.contains(allFileNames, subFileNames.get(i))) {
				getStoreFileVersionHistory(pathName + subFileNames.get(i), subFileRefs.get(i), accessToken);
				globalFileCounter++;
			}
		}
		if (subFolderNames.size() == 0) 
  			return;
  		
  		for (int i = 0; i < subFolderNames.size(); i++) {
  			String subFolderName = subFolderNames.get(i);
  			traverseFolder(subFolderContents.get(i), accessToken, pathName += subFolderName + "\\");
  		}
    }
    **/
    
    private static void traverseFolder(String contentsURL, String accessToken) throws XPathExpressionException, HttpException, IOException {
    	ArrayList<String> foldersTraversed = new ArrayList<String>();
    	//ArrayList<String> folderURLSTraversed = new ArrayList<String>();
    	GetMethod get = new GetMethod(contentsURL);
  		get.addRequestHeader("Authorization", accessToken);
  		client.executeMethod(get);
  		byte[] responseBody = get.getResponseBody();
  		String folderContents = new String(responseBody);
  		List<String> subFolderNames = XmlUtil.getNodeValues(folderContents, "/collectionContents/collection[@type='folder']/displayName/text()");
  		List<String> subFolderContents = XmlUtil.getNodeValues(folderContents, "/collectionContents/collection[@type='folder']/contents/text()");
  		List<String> subFileNames = XmlUtil.getNodeValues(folderContents, "/collectionContents/file/displayName/text()");
  		List<String> subFileRefs = XmlUtil.getNodeValues(folderContents, "/collectionContents/file/ref/text()");
  		ArrayList<List<String>> cur_subFolderNames = new ArrayList<List<String>>();
  		cur_subFolderNames.add(subFolderNames);
  		ArrayList<List<String>> cur_subFolderContents = new ArrayList<List<String>>();
  		cur_subFolderContents.add(subFolderContents);
  		ArrayList<List<String>> cur_subFileNames = new ArrayList<List<String>>();
  		cur_subFileNames.add(subFileNames);
  		ArrayList<List<String>> cur_subFileRefs = new ArrayList<List<String>>();
  		cur_subFileRefs.add(subFileRefs);
    	for (int i = 0; i < allPaths.size(); i++) {
    		boolean found = false;
    		ArrayList<String> currPath = allPaths.get(i);
    		//if I'm already somewhere, check to see if folder or file is where I'm at
    		if (foldersTraversed.size() > 0) {
    			while (foldersTraversed.size() > currPath.size()) {
    				cur_subFolderNames.remove(foldersTraversed.size());
    				cur_subFolderContents.remove(foldersTraversed.size());
    				cur_subFileNames.remove(foldersTraversed.size());
    				cur_subFileRefs.remove(foldersTraversed.size());
    				foldersTraversed.remove(foldersTraversed.size()-1);
    			}
    			
    			for (int j = foldersTraversed.size()-1; j >= 0; j--) {
    				//check to see if we have a match between how far we've traversed and the curr path
    				if (foldersTraversed.get(j).equalsIgnoreCase(currPath.get(j))) {
    					found = traverseHelper(foldersTraversed,cur_subFolderNames,cur_subFolderContents,cur_subFileNames,cur_subFileRefs,i);
    					break;
    				}
    				cur_subFolderNames.remove(j+1);
    				cur_subFolderContents.remove(j+1);
    				cur_subFileNames.remove(j+1);
    				cur_subFileRefs.remove(j+1);
    				foldersTraversed.remove(j);
    			}
    			//if there is never a match, call helper function to find file and store
    		} 
    		if (!found) {
    			//cases where foldersTraversed was originally 0 or if went through list and didn' find
    			found = traverseHelper(foldersTraversed,cur_subFolderNames,cur_subFolderContents,cur_subFileNames,cur_subFileRefs,i);
    		}
    		if (!found) {
    			//if stillll not found
    			System.out.println(":( Couldn't find file: " + currPath + "\\" + allFileNames.get(i));
    		}
    	}
    }
    
    //foldersTraversed size should always be one greater
    private static boolean traverseHelper(ArrayList<String> foldersTraversed, ArrayList<List<String>> cur_subFolderNames,
    		ArrayList<List<String>> cur_subFolderContents, ArrayList<List<String>> cur_subFileNames, ArrayList<List<String>> cur_subFileRefs, int i) throws HttpException, XPathExpressionException, IOException {
    	ArrayList<String> currPath = allPaths.get(i);
    	String fileName = allFileNames.get(i);
    	if ((foldersTraversed.size()+1) != cur_subFolderNames.size()) {
    		System.out.println("uh oh, constraint failed in traverseHelper, folder sizes mismatched");
    	}
    	boolean found = false;
    	while (!found) {
    		List<String> subFileNames = cur_subFileNames.get(cur_subFileNames.size()-1);
    		List<String> subFileRefs = cur_subFileRefs.get(cur_subFileRefs.size()-1);
    		//now we look for the file in all directories
    		for (int j = 0; j < subFileNames.size(); j++) {
    			int indexLoc = Parser.contains(subFileNames, fileName);
    			if (indexLoc != -1) {
    				//getStoreFileVersionHistory(foldersTraversed.toString() + "\\" + fileName, subFileRefs.get(indexLoc), globalToken);
    				//allFileRefURLs.add(subFileRefs.get(indexLoc));
    				found = true;
    				globalFileCounter++;
    				break;
    			}
    		}
    		if (foldersTraversed.size() >= currPath.size()) {
    			break;
    		}
    		String nextDirectory = currPath.get(foldersTraversed.size());
    		List<String> subFolderNames = cur_subFolderNames.get(foldersTraversed.size());
    		List<String> subFolderContents = cur_subFolderContents.get(foldersTraversed.size());
    		int indexFolderLoc = Parser.contains(subFolderNames, nextDirectory);
    		if (indexFolderLoc == -1) {
    			System.out.println("uh oh, folder not there!");
    			found = false;
    			break;
    		} else {
    			foldersTraversed.add(subFolderNames.get(indexFolderLoc));
    			GetMethod get = new GetMethod(subFolderContents.get(indexFolderLoc));
    	  		get.addRequestHeader("Authorization", globalToken);
    	  		client.executeMethod(get);
    	  		byte[] responseBody = get.getResponseBody();
    	  		String folderContents = new String(responseBody);
    	  		cur_subFolderNames.add(XmlUtil.getNodeValues(folderContents, "/collectionContents/collection[@type='folder']/displayName/text()"));
    	  		cur_subFolderContents.add(XmlUtil.getNodeValues(folderContents, "/collectionContents/collection[@type='folder']/contents/text()"));
    	  		cur_subFileNames.add(subFileNames = XmlUtil.getNodeValues(folderContents, "/collectionContents/file/displayName/text()"));
    	  		cur_subFileRefs.add(subFileRefs = XmlUtil.getNodeValues(folderContents, "/collectionContents/file/ref/text()"));
    		}
    		
    	}
    	return found;
    }
    
    /** Stores version history information in fileToVersHistory HashMap 
     * maps file names to version history in XML format, to later be processed
     * 
     * @param name name of the paths and file
     * @param ref ref URL of the file to get version history throw two GETs
     * @param token
     * @throws HttpException
     * @throws IOException
     * @throws XPathExpressionException
     */
    private static void getStoreFileVersionHistory(String name, String ref, String token) throws HttpException, IOException, XPathExpressionException {
  		GetMethod get = new GetMethod(ref);
  		get.addRequestHeader("Authorization", token);
  		client.executeMethod(get);
  		byte[] responseBody = get.getResponseBody();
  		String fileInfo = new String(responseBody);
  		List<String> fileVersions = XmlUtil.getNodeValues(fileInfo, "/file/versions/text()");
  		/**
  		String fileData = XmlUtil.getNodeValues(fileInfo, "/file/fileData/text()").get(0);
  		List<String> fileDatas = XmlUtil.getNodeValues(fileInfo, "/file/fileData/text()");
  		if (fileDatas.size()>1) {
  			String fileDataOlder = XmlUtil.getNodeValues(fileInfo, "/file/fileData/text()").get(1);
  			String[] splitt = name.split("\\\\");
  	  		FileDownloadAPI.downloadFileData(fileDataOlder, "C:\\Users\\George Wu\\Documents\\SugarSync Resources\\Version History Program Output\\Older Files\\" + splitt[splitt.length-1], token);
  	  		System.out.println("downloaded " + name);
  		} else {
  			System.out.println("only one version: " + name);
  		}
  		**/
  		//should be only one reference
  		String versionUrl = fileVersions.get(0);
  		//System.out.println(subFolderNames);
  		get = new GetMethod(versionUrl);
  		get.addRequestHeader("Authorization", token);
  		client.executeMethod(get);
  		responseBody = get.getResponseBody();
  		String versionInfo = new String(responseBody);
  		fileToVersHistory.put(name, versionInfo);
    }
    
    //writes a lot of information, and also downloads files
    private static void writeInfoFromVersHistory() throws XPathExpressionException, IOException {
    	File file1 = new File("C:\\Users\\George Wu\\Documents\\SugarSync Resources\\Version History Program Output\\MoreThanOneVersHistory.txt");
    	File file2 = new File("C:\\Users\\George Wu\\Documents\\SugarSync Resources\\Version History Program Output\\OnlyOneVersHistory.txt");
    	FileWriter fw1 = new FileWriter(file1.getAbsoluteFile());
    	FileWriter fw2 = new FileWriter(file2.getAbsoluteFile());
		BufferedWriter bw1 = new BufferedWriter(fw1);
		BufferedWriter bw2 = new BufferedWriter(fw2);
    	Iterator it = fileToVersHistory.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            String pathAndFile = (String) pairs.getKey();
            String versionInfo = (String) pairs.getValue();
            List<String> fileVersionPresents = XmlUtil.getNodeValues(versionInfo, "/fileVersions/fileVersion/presentOnServer/text()");
            List<String> lastModifiedTimes = XmlUtil.getNodeValues(versionInfo, "/fileVersions/fileVersion/lastModified/text()");
            List<String> fileDatas = XmlUtil.getNodeValues(versionInfo, "/fileVersions/fileVersion/fileData/text()");
            if (fileDatas.size()>1) {
      			String fileDataOlder = fileDatas.get(1);
      			String[] splitt = pathAndFile.split("\\\\");
      	  		FileDownloadAPI.downloadFileData(fileDataOlder, "C:\\Users\\George Wu\\Documents\\SugarSync Resources\\Version History Program Output\\Older Files\\" + splitt[splitt.length-1], globalToken);
      	  		System.out.println("downloaded " + pathAndFile);
      		} else {
      			bw2.write("only one version: " + pathAndFile);
      			System.out.println("only one version: " + pathAndFile);
      		}
            int numOnServer = 0;
            
            for (int i = 0; i < fileVersionPresents.size(); i++) {
            	if (fileVersionPresents.get(i).equals("true")) {
            		numOnServer++;
            	}
            }
            if (numOnServer > 1) {
            	bw1.write(pathAndFile + " : " + numOnServer + " histories\n");
            	for (int j = 0; j < lastModifiedTimes.size(); j++) {
            		if (fileVersionPresents.get(j).equals("true")) {
            			bw1.write("       " + lastModifiedTimes.get(j) + "\n");
            		}
            	}
            } else if (numOnServer == 1) {
            	bw2.write(pathAndFile + "\n");
            	for (int j = 0; j < lastModifiedTimes.size(); j++) {
            		if (fileVersionPresents.get(j).equals("true")) {
            			bw2.write("       " + lastModifiedTimes.get(j) + "\n");
            		}
            	}
            } else {
            	bw2.write(pathAndFile + ": None on server\n");
            }
            it.remove(); // avoids a ConcurrentModificationException
            
        }
    	bw1.close();
    	bw2.close();
    	fw1.close();
    	fw2.close();
    	System.out.println("Done writing!");
    	
    }
    
    //download all data from the arraylist of references gotten from parser
    private static void downloadLatestVersions() throws HttpException, IOException, XPathExpressionException {
    	allFileRefURLs = Parser.readRefsFromDisk();
    	System.out.println("Size of allFileRefURLs ArrayList is : " + allFileRefURLs.size());
    	int status = 0;
    	for (String ref : allFileRefURLs) {
	    	GetMethod get = new GetMethod(ref);
	  		get.addRequestHeader("Authorization", globalToken);
	  		client.executeMethod(get);
	  		byte[] responseBody = get.getResponseBody();
	  		String fileInfo = new String(responseBody);
	  		String fileDisplayName = XmlUtil.getNodeValues(fileInfo, "/file/displayName/text()").get(0);
	  		List<String> fileVersions = XmlUtil.getNodeValues(fileInfo, "/file/versions/text()");
	  		//should be only one reference
	  		String versionUrl = fileVersions.get(0);
	  		//System.out.println(subFolderNames);
	  		get = new GetMethod(versionUrl);
	  		get.addRequestHeader("Authorization", globalToken);
	  		client.executeMethod(get);
	  		responseBody = get.getResponseBody();
	  		String versionInfo = new String(responseBody);
	  		List<String> fileVersionPresents = XmlUtil.getNodeValues(versionInfo, "/fileVersions/fileVersion/presentOnServer/text()");
            List<String> lastModifiedTimes = XmlUtil.getNodeValues(versionInfo, "/fileVersions/fileVersion/lastModified/text()");
            List<String> fileDatas = XmlUtil.getNodeValues(versionInfo, "/fileVersions/fileVersion/fileData/text()");
  	  		FileDownloadAPI.downloadFileData(fileDatas.get(0), "C:\\Users\\George Wu\\Documents\\SugarSync Resources\\SugarSync Files Dump\\" + fileDisplayName, globalToken);
  	  		FileDownloadAPI.downloadFileData(fileDatas.get(1), "C:\\Users\\George Wu\\Documents\\SugarSync Resources\\SugarSync Older Files Dump\\" + fileDisplayName, globalToken);
  	  		System.out.println(status);
  	  		status++;
    	}
    	System.out.println("done downloading");
    }
    
    private static void checkCorruptStatus() throws IOException, XPathExpressionException {
    	File file1 = new File("C:\\Users\\George Wu\\Documents\\SugarSync Resources\\Corrupt Status.txt");
    	FileWriter fw1 = new FileWriter(file1.getAbsoluteFile());
		BufferedWriter bw1 = new BufferedWriter(fw1);
		
		allFileRefURLs = Parser.readRefsFromDisk();
		
    	int status = 0;
    	for (String ref : allFileRefURLs) {
	    	GetMethod get = new GetMethod(ref);
	  		get.addRequestHeader("Authorization", globalToken);
	  		client.executeMethod(get);
	  		byte[] responseBody = get.getResponseBody();
	  		String fileInfo = new String(responseBody);
	  		String fileDisplayName = XmlUtil.getNodeValues(fileInfo, "/file/displayName/text()").get(0);
	  		List<String> fileVersions = XmlUtil.getNodeValues(fileInfo, "/file/versions/text()");
	  		//should be only one reference
	  		String versionUrl = fileVersions.get(0);
	  		
	  		//get versions list, including data, etc. 
	  		get = new GetMethod(versionUrl);
	  		get.addRequestHeader("Authorization", globalToken);
	  		client.executeMethod(get);
	  		responseBody = get.getResponseBody();
	  		String versionInfo = new String(responseBody);
	  		List<String> fileVersionPresents = XmlUtil.getNodeValues(versionInfo, "/fileVersions/fileVersion/presentOnServer/text()");
            List<String> lastModifiedTimes = XmlUtil.getNodeValues(versionInfo, "/fileVersions/fileVersion/lastModified/text()");
            List<String> fileDatas = XmlUtil.getNodeValues(versionInfo, "/fileVersions/fileVersion/fileData/text()");
            
            boolean latestCorrupt = FileDownloadAPI.downloadFileDataCheckCorrupt(fileDisplayName, fileDatas.get(0), globalToken); 
            boolean secondLatestCorrupt = true;
            if (latestCorrupt) {
            	secondLatestCorrupt = FileDownloadAPI.downloadFileDataCheckCorrupt(fileDisplayName, fileDatas.get(1), globalToken); 
            }
            if (!latestCorrupt) {
            	bw1.write(fileDisplayName + ", corrupt in latest version: " + latestCorrupt);
            } else {
            	bw1.write(fileDisplayName + ", corrupt in second latest version: " + secondLatestCorrupt);
            }
            if (status%20 == 0)
  	  			System.out.println(status);
            if (latestCorrupt && secondLatestCorrupt) {
            	System.out.println("both latest and second latest corrupt :( " + fileDisplayName);
            } else {
            	System.out.println("latest: " + latestCorrupt);
            	System.out.println("second latest: " + secondLatestCorrupt);
            }
  	  		status++;
    	}
    	bw1.close();
    	fw1.close();
    	System.out.println("done checking");
    }

    /**
     * Prints the files and folders from the xml response
     * 
     * @param responseBody
     *            the xml server response
     */
    private static void printFolderContents(String responseBody) {
        try {
            List<String> folderNames = XmlUtil.getNodeValues(responseBody,
                    "/collectionContents/collection[@type=\"folder\"]/displayName/text()");
            List<String> fileNames = XmlUtil.getNodeValues(responseBody,
                    "/collectionContents/file/displayName/text()");
            System.out.println("\n-Magic Briefcase");
            System.out.println("\t-Folders:");
            for (String folder : folderNames) {
                System.out.println("\t\t" + folder);
            }
            System.out.println("\t-Files:");
            for (String file : fileNames) {
                System.out.println("\t\t" + file);
            }
        } catch (XPathExpressionException e1) {
            System.out.println("Error while printing the folder contents:");
            System.out.println(responseBody);
        }
    }

    /**
     * Handles "download" command.
     * 
     * 1. Get the "Magic Briefcase" contents
     * 
     * 2. Check if the specified file exists in the "Magic Briefcase" folder and
     * retrieve its file data link
     * 
     * 3. Make a HTTP GET request to the previous extracted link and save the
     * response content to a local file
     * 
     * @param accessToken
     *            the access token
     * @param file
     *            the remote file within the "Magic Briefcase" folder
     * @throws IOException
     * @throws XPathExpressionException
     */
    private static void handleDownloadCommand(String accessToken, String file) throws IOException,
            XPathExpressionException {
        HttpResponse magicBriefcaseContents = getMagicBriefcaseFolderContents(accessToken);

        List<String> fileDataLink = XmlUtil.getNodeValues(magicBriefcaseContents.getResponseBody(),
                "collectionContents/file[displayName=\"" + file + "\"]/fileData/text()");
        if (fileDataLink.size() == 0) {
            System.out.println("\nFile " + file + " not found in Magic Briefcase folder");
            System.exit(0);
        }
        HttpResponse fileDownloadResponse = FileDownloadAPI.downloadFileData(fileDataLink.get(0), file,
                accessToken);
        validateHttpResponse(fileDownloadResponse);

        System.out.println("\nDownload completed successfully. The " + file
                + " from \"Magic Briefcase\" was downloaded to the local directory.");
    }
    

    /**
     * Handles "upload" tool command.
     * 
     * 1. Get the user information
     * 
     * 2. Extract the "Magic Briefcase" folder link from the user information
     * response
     * 
     * 3. Creates a file representation in "Magic Briefcase" folder
     * 
     * 4. Uploads the file data associated to the previously created file
     * representation
     * 
     * @param accessToken
     *            the access token
     * @param file
     *            the local file that will be uploaded in "Magic Briefcase"
     * @throws XPathExpressionException
     * @throws IOException
     */
    private static void handleUploadCommand(String accessToken, String file)
            throws XPathExpressionException, IOException {
        if (!(new File(file).exists())) {
            System.out.println("\nFile " + file + "  doesn not exists in the current directory");
            System.exit(0);
        }
        HttpResponse userInfoResponse = getUserInfo(accessToken);

        String magicBriefcaseFolderLink = XmlUtil.getNodeValues(userInfoResponse.getResponseBody(),
                "/user/magicBriefcase/text()").get(0);

        HttpResponse resp = FileCreation.createFile(magicBriefcaseFolderLink, file, "", accessToken);

        String fileDataUrl = resp.getHeader("Location").getValue() + "/data";
        resp = FileUploadAPI.uploadFile(fileDataUrl, file, accessToken);

        System.out.println("\nUpload completed successfully. Check \"Magic Briefcase\" remote folder");

    }

    // ---Print and validation
    /**
     * Validates the input arguments
     * 
     * @param args
     *            the arguments passed to main method
     */
    private static void validateArgs(List<String> args) {
        if (args.size() != 11 && args.size() != 12) {
            printUsage();
            System.exit(0);
        }
    }

    /**
     * Validates the HTTP response. If HTTP response status code indicates an
     * error the details are printed and the tool exists
     * 
     * @param httpResponse
     *            the HTTP response which will be validated
     */
    private static void validateHttpResponse(HttpResponse httpResponse) {
        if (httpResponse.getHttpStatusCode() > 299) {
            System.out.println("HTTP ERROR!");
            printResponse(httpResponse);
            System.exit(0);
        }
    }

    /**
     * Prints the http response
     * 
     * @param response
     *            the HTTP response
     */
    private static void printResponse(HttpResponse response) {
        System.out.println("STATUS CODE: " + response.getHttpStatusCode());
        // if the response is in xml format try to pretty format it, otherwise
        // leave it as it is
        String responseBodyString = null;
        try {
            responseBodyString = XmlUtil.formatXml(response.getResponseBody());
        } catch (Exception e) {
            responseBodyString = response.getResponseBody();
        }
        System.out.println("RESPONSE BODY:\n" + responseBodyString);
    }

    /**
     * Prints the tool usage
     */
    private static void printUsage() {
        System.out.println("USAGE:");
        System.out.println("java -jar sample-tool.jar " + userParam + " <username> " + passParam
                + " <password> " +applicationIdParam+" <appId> "+ accesskeyParam + " <publicAccessKey> " + privateaccesskeyParam
                + " <privateAccessKey> " + " ( " + quotaCmd + " | " + listCmd + " | " + downloadCmd
                + " <fileToDownload> | " + uploadCmd + " <fileToUpload> )");
        System.out.println("\nWHERE:");
        System.out.println("<username> - SugarSync username (email address)");
        System.out.println("<password> - SugarSync password");
        System.out.println("<appId> - The id of the app created from developer site");
        System.out.println("<publicAccessKey> - Developer accessKey");
        System.out.println("<privateAccessKey> - Developer privateAccessKey");
        System.out.println("<fileToDownload> - The file from default \"Magic Briefcase\" folder that you want to download");
        System.out.println("<fileToUpload> - The file from current directory that you want to upload into default \"Magic Briefcase\" folder ");
        

        System.out.println("\nEXAMPLES:");
        System.out.println("\nDisplaying user quota:");
        System.out.println("java -jar sample-tool.jar " + userParam + " user@email.com " +passParam
                + " userpassword " +applicationIdParam+" /sc/10016/3_1640259 "+ accesskeyParam + " MTUzOTEyNjEzMjM4NzEwNDg0MTc " + privateaccesskeyParam
                + " ZmNhMWY2MTZlY2M1NDg4OGJmZDY4OTExMjY5OGUxOWY " + quotaCmd);

        System.out.println("\nListing \"Magic Briefcase\" folder contents:");
        System.out.println("java -jar sample-tool.jar " + userParam + " user@email.com " + passParam
                + " userpassword " +applicationIdParam+" /sc/10016/3_1640259 "+ accesskeyParam + " MTUzOTEyNjEzMjM4NzEwNDg0MTc " + privateaccesskeyParam
                + " ZmNhMWY2MTZlY2M1NDg4OGJmZDY4OTExMjY5OGUxOWY " + listCmd);

        System.out.println("\nDownloading \"file.txt\" file  from \"Magic Briefcase\"");
        System.out.println("java -jar sample-tool.jar " + userParam + " user@email.com " + passParam
                + " userpassword " +applicationIdParam+" /sc/10016/3_1640259 "+ accesskeyParam + " MTUzOTEyNjEzMjM4NzEwNDg0MTc " + privateaccesskeyParam
                + " ZmNhMWY2MTZlY2M1NDg4OGJmZDY4OTExMjY5OGUxOWY " + downloadCmd + " file.txt");
        System.out.println("Please not that \"file.txt\" must exists in \"Magic Briefcase\" remote folder");

        System.out.println("\nUploading \"uploadFile.txt\" file  to \"Magic Briefcase\"");
        System.out.println("java -jar sample-tool.jar " + userParam + " user@email.com " + passParam
                + " userpassword " +applicationIdParam+" /sc/10016/3_1640259 "+ accesskeyParam + " MTUzOTEyNjEzMjM4NzEwNDg0MTc " + privateaccesskeyParam
                + " ZmNhMWY2MTZlY2M1NDg4OGJmZDY4OTExMjY5OGUxOWY " + downloadCmd + " file.txt");
        System.out.println("Please not that \"uploadFile.txt\" must exists in the local directory");

    }

    public static void main(String[] args) {
        List<String> argumentList = Arrays.asList(args);

        validateArgs(argumentList);

        String username = getParam(userParam, argumentList);
        String password = getParam(passParam, argumentList);
        String applicationId = getParam(applicationIdParam, argumentList);
        String accessKey = getParam(accesskeyParam, argumentList);
        String privateAccessKey = getParam(privateaccesskeyParam, argumentList);
        

        try {
            String refreshToken = getRefreshToken(username, password, applicationId, accessKey, privateAccessKey);
            
            String accessToken = getAccessToken(accessKey,privateAccessKey,refreshToken);
            
            String command = getCommand(argumentList);

            if (command.equals(quotaCmd)) {
                handleQuotaCommand(accessToken);
            } else if (command.equals(listCmd)) {
                handleListCommand(accessToken);
            } else if (command.equals(downloadCmd)) {
                String file = argumentList.get(argumentList.size() - 1);
                handleDownloadCommand(accessToken, file);
            } else if (command.equals(uploadCmd)) {
                String file = argumentList.get(argumentList.size() - 1);
                handleUploadCommand(accessToken, file);
                
                
                
                
            } else if (command.equals(getReceivedShareInfo)) {
            	Parser parser = new Parser();
            	parser.readFile();
            	client = new HttpClient();
            	allPaths = parser.getAllPaths();
            	allFileNames = parser.getAllFileNames();
            	allFileRefURLs = new ArrayList<String>();
            	//fileToVersHistory = new HashMap<String, String>();
            	globalFileCounter = 0;
            	
            	String receivedShareInfo = handleGetReceivedShareInfo(accessToken);
            	String folderInfo = getFolderInformation(receivedShareInfo, "B_Product_Starter_Account", accessToken);
            	//got shared folder, name is B_Product_Starter_Account
            	//traverse, all the while updating the name so we can check the hashmap
            	//traverseFolder(folderInfo, accessToken);
            	//System.out.println("done, globalCounter is " + globalFileCounter);
            	//writeInfoFromVersHistory();
            	//Parser.writeToDisk(allFileRefURLs);
            	//downloadLatestVersions();
            	checkCorruptStatus();
            	
            } else {
                System.out.println("Uknown command: " + command);
                printUsage();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   

}
