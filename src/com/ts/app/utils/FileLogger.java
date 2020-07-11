package com.ts.app.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class FileLogger {
	
	private BufferedWriter writer;
	
	private File file;
	
	public FileLogger(String fileName) throws IOException {
		file = new File(fileName);
		writer = new BufferedWriter(new FileWriter(file));
	}
	
	public void log(String str,boolean... isSingleLine) throws IOException {
		writer.write(new Date().toString()+" :: "+str+"\n");
		if(isSingleLine.length>0 && isSingleLine[0]) {
			str+="\r";
		}
		else {
			str+="\n";
		}
		System.out.print(new Date().toString()+" :: "+str);
	}
	
	public void close() throws IOException {
		writer.close();
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		this.close();
	}

}
