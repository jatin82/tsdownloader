package com.ts.app;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;

import com.ts.app.utils.FileLogger;
import com.ts.app.utils.TerminalCMD;

public class MainApp {

//	static String configFile = "/Users/b0216204/Downloads/movie/tsconfig/config_test.properties";
	
	static String configFile = "config.properties";
	
	static String tsFiles = "ts.file.path";
	
	static String referer = "ts.referer";
	
	static String tsUrl = "ts.url";
	
	static String fileToSavePath = "file.save.path";
	
	static String fileName = "file.name";
	
	static String tsFileConcatFileName = "ts.file.concat.file.name";
	
	static String ffmpegCommand = "ffmpeg.command";
	
	static String postCompleteCommands = "post.complete.command";
	
	static TerminalCMD terminalCMD;
	
	static Properties properties = new Properties();
	
	public static void main(String[] args) {
	
		if(args.length>0) {
			configFile = args[0];
		}
		
		try {
			loadProperties();
			terminalCMD = new TerminalCMD(true, new FileLogger(properties.getProperty(fileToSavePath)+"/log.txt"));
			List<String> tsFiles = download();
			mergeAllTSFiles(tsFiles);
			
			terminalCMD.run(properties.getProperty(postCompleteCommands));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void loadProperties() throws IOException {	
		FileInputStream fis = new FileInputStream(configFile);
		properties.load(fis);
			
	}
	
	public static void mergeAllTSFiles(List<String> tsFiles) throws IOException, InterruptedException {
		logMergeFiles(tsFiles);
		terminalCMD.run(properties.getProperty(ffmpegCommand));
	}
	
	public static void logMergeFiles(List<String> tsFiles) throws IOException {
		FileWriter fileWriter = new FileWriter(new File(
				properties.getProperty(fileToSavePath)+"/out/"+properties.getProperty(tsFileConcatFileName)));
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		for(String file:tsFiles) {
			bufferedWriter.write(file+"\n");
		}
		bufferedWriter.close();
	}
	
	public static List<String> download() throws Exception {
		List<String> tsFileNames = parseTsFileNames(); 
		
		String baseURL = properties.getProperty(tsUrl);
		String refHeader = properties.getProperty(referer);
		String baseFileToSave = properties.getProperty(fileToSavePath);
		for(int i=0;i<tsFileNames.size();i++) {
			String file  = tsFileNames.get(i);
			String progress = ((float)((int)(((float)(i+1)/(float)tsFileNames.size())*10000F))/100F)+"";
			
			String fileLocation = baseFileToSave+"/out/"+file;
			if(!isFilePresent(fileLocation)) {
				long start = System.currentTimeMillis();
				downloadTS(baseURL+"/"+file,refHeader, fileLocation );
				long end = System.currentTimeMillis();
				
				String speed = formatDecimal(calculateSpeed(start,end,new File(fileLocation).getUsableSpace()),2);
				terminalCMD.log(" [DOWNLOAD] :: "+file+" completed :: "+progress+" % -- "+speed+" kb/s",true);
			}
			else {
				terminalCMD.log("SKIPPED [DOWNLOAD] :: "+file+" completed :: "+progress+" %",true);
			}
		}
		return tsFileNames.parallelStream().map(t->{
			return "file '"+baseFileToSave+"/out/"+t+"'";
		}).collect(Collectors.toList());
	}
	
	public static double calculateSpeed(long start, long end, long spaceOccupied) {
		spaceOccupied /= Math.pow(10, 9);
		long seconds = (end - start)/1000;
		double speed = (double)spaceOccupied/seconds;
		return speed;
	}
	
	public static String formatDecimal(double decimalValue, int roundDigit) {
		double roundOff = Math.round(decimalValue * Math.pow(10, roundDigit)) / Math.pow(10, roundDigit);
		return roundOff+"";
	}
	
	public static boolean isFilePresent(String fileLocation) {
		File file = new File(fileLocation);
		return file.exists();
	}
	
	public static void downloadTS(String url, String refHeader, String fileLocation) throws Exception {
		HttpClient client = HttpClients.custom().build();
		HttpUriRequest request = RequestBuilder.get()
		  .setUri(url)
		  .setHeader("referer", refHeader)
		  .build();
		
		HttpResponse response = client.execute(request);
		
		if(response.getStatusLine().getStatusCode()!=200) {
			throw new Exception("TS file download failed reponsse code:"+response.getStatusLine().getStatusCode()+" URL:"+url);
		}
		
		
		HttpEntity entity = response.getEntity();
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
