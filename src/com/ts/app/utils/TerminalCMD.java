package com.ts.app.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TerminalCMD {
	
	private final static String JOINER = " & ";
	
	private List<String> commands;
	
	private FileLogger logger;
	
	public TerminalCMD(boolean isLinux, FileLogger logger) {
		this.commands = new ArrayList<>();
		if(isLinux) {
			this.commands.add("sh");
			this.commands.add("-c");
		}else {
			this.commands.add("sh");
			this.commands.add("-c");
		}
		this.logger = logger;
	}
	
	public void log(String str,boolean... isSingleLine) {
		try {
			logger.log(str,isSingleLine);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String runBulk(List<String> commands) throws IOException, InterruptedException {
		ProcessBuilder builder = getDefualtBuilder();
		builder.command().add(concatAllCommands(commands));
		String res = run(builder);
		return res;
	}
	
	public String runInSequence(List<String> commands) throws IOException, InterruptedException {
		StringBuilder res = new StringBuilder();
		for(String command : commands) {
			ProcessBuilder builder = getDefualtBuilder();
			builder.command().add(command);
			String out = run(builder);
			res.append(out);
		}
		return res.toString();
	}
	
	private String concatAllCommands(List<String> commands) {
		return commands.stream().collect(Collectors.joining(JOINER));
	}
	
	
	public String run(String command) throws IOException, InterruptedException {
		ProcessBuilder builder = getDefualtBuilder();
		builder.command().add(command);
		return run(builder);
	}
	
	
	public String run(ProcessBuilder builder) throws IOException, InterruptedException {
		Process process = builder.start();
		String res = run(process);
		process.destroy();
		return res;
	}
	
	public String run(Process process) throws IOException, InterruptedException {
        BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line= r.readLine())!=null) {
            logger.log(line,false);
            sb.append(line+"\n");
        }
        return sb.toString();
	}
	
	private ProcessBuilder getDefualtBuilder() {
		ProcessBuilder builder = new ProcessBuilder();
		builder.redirectErrorStream(true);
		builder.command().add(this.commands.get(0));
		builder.command().add(this.commands.get(1));
		return builder;
	}

}