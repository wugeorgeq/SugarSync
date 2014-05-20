package com.sugarsync.sample.george;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
 
public class Parser {
	
	private static final String path = "C:\\Users\\George Wu\\Documents\\SugarSync Resources\\B_Product_Firefly_Files.txt";
	private static ArrayList<String> paths = new ArrayList<String>();
	//key of hashMap is the path, including folders after B_Product_Firefly, and not including file name
	//val is list of file names
	//key: 5_Sales & Marketing\4_Collateral\FF Enrollment Kit\Assets\
	//val: FireflyLogo_Black.ai
	private static HashMap<String, ArrayList<String>> directories = new HashMap<String, ArrayList<String>>();
	private static String allFolderNames;
	
	
	private static ArrayList<ArrayList<String>> allPaths;
	private static ArrayList<String> fileNames = new ArrayList<String>();
	private static int counter;
	
	//set of extensions
	private static HashSet<String> extensions;
	/**
	 *  psd		38 42 50 53
		DOCX    50 4B
		jpg     FF D8 FF
		xlsx    50 4B
		ai      25 50
		pptx    50 4B
		pdf     25 50 44 46
		indd    6  6
		ppt     D0 CF
		doc     D0 CF
		xls     D0 CF
	 *
	 */
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
	public Parser() {
		this.path = "C:\\Users\\George Wu\\Documents\\SugarSync Resources\\B_Product_Firefly_Files.txt";
		this.paths = new ArrayList<String>();
	}
	**/
	
	public static void main(String[] args) {
		/**
		String testPath = "yo.aha";
		String[] splitted = testPath.split("\\.");
		System.out.println(splitted[0]);
		System.out.println(splitted[1]);
		**/
		
		//Parser parser = new Parser();
		//Parser.readFile();
		Parser.readFileByteArray();
		/**
		ArrayList<String> testy = new ArrayList<String>();
		testy.add("one");
		testy.add("two");
		testy.add("three");
		testy.add("Four");
		System.out.println(testy);
		System.out.println(contains(testy, "hree"));
		**/
		//String hex = "\u0048ff";
		
		
	}
 
	public static void readFile() {
		paths = new ArrayList<String>();
		directories = new HashMap<String, ArrayList<String>>();
		allFolderNames = "";
		fileNames = new ArrayList<String>();
		allPaths = new ArrayList<ArrayList<String>>();
		//extensions = new HashSet<String>();
		counter = 0;
 
		BufferedReader br = null;
 
		try {
 
			String sCurrentLine;
 
			br = new BufferedReader(new FileReader(path));
 
			while ((sCurrentLine = br.readLine()) != null) {
				//paths.add(sCurrentLine);
				//only need the path after B_Product_Firefly\
				//		ex: 1_Product Spec\01_Account Delivery\Analysis & Summaries\130610_Alpha_Bnk_Product_v.1.pptx
				String subPath = sCurrentLine.substring(88);
				String[] folderNames = subPath.split("\\\\");
				//String[] getExt = subPath.split("\\.");
				//extensions.add(getExt[getExt.length-1]);
				String hashKey = "";
				String hashVal = "";
				ArrayList<String> pathNames = new ArrayList<String>();
				for (int i = 0; i < folderNames.length; i++) {
					if (i == folderNames.length-1) {
						hashVal = folderNames[i];
					} else {
						hashKey += folderNames[i] + "\\";
						pathNames.add(folderNames[i]);
						if (!allFolderNames.contains(folderNames[i])) {
							allFolderNames += folderNames[i] + "\\";
						}
					}
				}
				allPaths.add(pathNames);
				
				//System.out.println(hashKey);
				paths.add(hashKey + hashVal);
				fileNames.add(hashVal);
				if (directories.containsKey(hashKey)) {
					ArrayList<String> currVal = directories.get(hashKey);
					currVal.add(hashVal);
					//System.out.println(hashVal);
				} else {
					ArrayList<String> oneVal = new ArrayList<String>();
					oneVal.add(hashVal);
					directories.put(hashKey, oneVal);
					//System.out.println(oneVal);
				}
				//System.out.println(hashKey);
				counter++;
				
			}
 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		/**
		//System.out.println(counter);
		for (String ext : extensions) {
			System.out.println(ext);
		}
		**/
		System.out.println("done with readFile()");
	}
	
	public ArrayList<String> getPaths() {
		return paths;
	}
	
	public HashMap<String, ArrayList<String>> getDirectories() {
		return directories;
	}
	
	public String getAllFolderNames() {
		return allFolderNames;
	}
	
	public ArrayList<String> getAllFileNames() {
		return fileNames;
	}
	
	public ArrayList<ArrayList<String>> getAllPaths() {
		return allPaths;
	}
	
	//returns index for true, -1 for false
	public static int contains(List<String> subFileNames, String input) {
		boolean contains = false;
		int i = 0;
		for (i = subFileNames.size()-1; i >= 0; i--) {
			if (input.equalsIgnoreCase(subFileNames.get(i))) {
		        contains = true;
		        break; // No need to look further.
		    } 
		}
		if (contains) {
			return i;
		} else {
			return -1;
		}
	}
	
	public static void writeToDisk(ArrayList<String> in) {
		try {
 
			File file = new File("C:\\Users\\George Wu\\Documents\\SugarSync Resources\\FileRefUrls.txt");
 
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
 
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for (int i = 0; i < in.size(); i++) {
				bw.write(in.get(i));
				bw.newLine();
			}
			
			bw.close();
 
			System.out.println("Done writing to FileRefUrls.txt");
 
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static ArrayList<String> readRefsFromDisk() {
		ArrayList<String> fileRefURLs = new ArrayList<String>();
		
		BufferedReader br = null;
		 
		try {
 
			String sCurrentLine;
 
			br = new BufferedReader(new FileReader("C:\\Users\\George Wu\\Documents\\SugarSync Resources\\FileRefUrls.txt"));
			
			int counter = 0;
			while (((sCurrentLine = br.readLine()) != null)&&(counter != 996)) {
				fileRefURLs.add(sCurrentLine);
				counter++;
			}
 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return fileRefURLs;
	}
	
	public static HashMap<String, Boolean> readOCFDataFromDisk(String file) { 
		HashMap<String, Boolean> OCFData = new HashMap<String, Boolean>();
		BufferedReader br = null;
		 
		try {
 
			String sCurrentLine;
 
			br = new BufferedReader(new FileReader(file));
			
			int counter = 0;
			while (((sCurrentLine = br.readLine()) != null)&&(counter != 996)) {
				
				counter++;
			}
 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return OCFData;
	}
	
	public static void readFileByteArray() {
		
		FileInputStream fileInputStream=null;
		 
        File file = new File("C:\\Users\\George Wu\\Documents\\SugarSync Resources\\SugarSync Older Files Dump\\Earnlogo.ai");
		//File file = new File("C:\\Users\\George Wu\\Desktop\\OFX Field Defs and Return Codes_RR-20130409 old.xls");
 
        byte[] bFile = new byte[4];
        //jpg ff d8 ff
        byte[] jpgSig = new byte[] {(byte) 0x25, (byte) 0x50, (byte) 0xff};
        System.out.println("jpgSig bytearray length : " + jpgSig.length);
        
        try {
	            //convert file into array of bytes
		    fileInputStream = new FileInputStream(file);
		    fileInputStream.read(bFile);
		    fileInputStream.close();
	 
		    for (int i = 0; i < bFile.length; i++) {
		       	//System.out.print((char)bFile[i]);
	            }
	 
		    System.out.println("Done reading file as byte");
		    System.out.println("jpgSig: " + jpgSig[0] + " " + jpgSig[1] + " " + jpgSig[2]);
		    System.out.println("bFile: " + bFile[0] + " " + bFile[1] + " " + bFile[2] + " " + bFile[3]);
		    // ^ they're equal!
		    
        }catch(Exception e){
        	e.printStackTrace();
        }
        
	}
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	
	public static boolean isCorrupt(String filePath) {
		
		return false;
	}
	
	
}