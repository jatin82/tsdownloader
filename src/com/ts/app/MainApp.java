package com.ts.app;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;

public class MainApp {

	static String configFile = "/Users/b0216204/Downloads/movie/tsconfig/config.properties";
	
	static String tsFiles = "ts.file.path";
	
	static String referer = "ts.referer";
	
	static String tsUrl = "ts.url";
	
	static String fileToSavePath = "file.save.path";
	
	static String fileName = "file.name";
	
	
	static Properties properties = new Properties();
	
	public static void main(String[] args) {
	
		if(args.length>0) {
			configFile = args[0];
		}
		loadProperties();
		
		try {
			download();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void loadProperties() {
		try {
			FileInputStream fis = new FileInputStream(configFile);
			properties.load(fis);
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void download() throws IOException {
		List<String> tsFileNames = parseTsFileNames(); 
		
		String baseURL = properties.getProperty(tsUrl);
		String refHeader = properties.getProperty(referer);
		String baseFileToSave = properties.getProperty(fileToSavePath);
		for(int i=0;i<tsFileNames.size();i++) {
			String file  = tsFileNames.get(i);
			String progress = ((float)((int)(((float)(i+1)/(float)tsFileNames.size())*10000F))/100F)+"";
			
			downloadTS(baseURL+"/"+file,refHeader, baseFileToSave+"/out/"+file);
			
			System.out.println(" [DOWNLOAD] :: "+file+" completed :: "+progress+" %");
			
		}
		
	}
	
	public static void downloadTS(String url, String refHeader, String fileLocation) throws ClientProtocolException, IOException {
		HttpClient client = HttpClients.custom().build();
		HttpUriRequest request = RequestBuilder.get()
		  .setUri(url)
		  .setHeader("referer", refHeader)
		  .build();
		
		HttpEntity entity = client.execute(request).getEntity();
		entity.getContentLength();
		InputStream stream =  entity.getContent();
		
		FileOutputStream fos = new FileOutputStream(fileLocation);
		int data;
		while((data=stream.read())!=-1) {
			fos.write(data);
		}
		fos.close();
		System.out.print("SUCCESS");
		
	}
	
	public static List<String> parseTsFileNames() throws IOException{
		FileReader reader = new FileReader(properties.getProperty(tsFiles));
		
		BufferedReader br = new BufferedReader(reader);
		
		List<String> files = new ArrayList<>();
		
		
		String read = null;
		while((read=br.readLine())!=null) {
			read = read.trim();
			if(read.charAt(0)!='#') {
				files.add(read);
			}
		}
		
		return files;
	}
}
