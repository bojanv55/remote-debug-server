package me.vukas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DebugProcess implements Runnable {
	private static final Logger LOG = Logger.getLogger("me.vukas.DebugProcess");
	private PrintWriter out;
	private Process process;
	private String command;
	private String workDir;
	private Integer port;
	private boolean stopped;

	private Pattern patternJava = Pattern.compile("\\s\\|\\s((?!bin/java.exe).*)bin/java.exe");

	DebugProcess(PrintWriter out, String command, Integer port) {
		this.out = out;
		this.command = command;
		this.port = port;
	}

	private String prepareCommand(){
		String command = this.command;

		command = command.replace("\\", "/");

		String rplc = "";
		Matcher matcherJava = patternJava.matcher(command);
		if (matcherJava.find()) {
			rplc = matcherJava.group(1);
		}

		command = command.replace(rplc, "/opt/jdk/");

		for(char c = 'A'; c<='Z'; c++){
			command = command.replace(c+":/", "/"+Character.toLowerCase(c)+"/");
		}

		command = command.replace("java.exe", "java -agentlib:jdwp=transport=dt_socket,address="+this.port+",suspend=y,server=y");
		command = command.replace(";", ":");

		String workDir = command.substring(0, command.indexOf(" | "));

		command = command.substring(command.indexOf(" | ")+3);

		this.workDir = workDir;

		//command = command.replace("classpath \"", "classpath \"" + workDir +":");

		return command;
	}

	private void start() throws IOException {

		String command = this.prepareCommand();

		String execCommand = "cd " + workDir + " && " + "exec " + command;
		ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", execCommand);
		pb.redirectErrorStream(true);
		if(!stopped) {
			LOG.info("Starting process: " + command);
			this.process = pb.start();
			this.writeOutput(this.process.getInputStream());
		}
	}

	private void writeOutput(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = br.readLine()) != null) {
			this.out.println(line);
		}
	}

	void stop() {
		this.stopped = true;
		if (this.process != null) {
			LOG.info("Stopping process on port " + this.port);
			this.process.destroy();
			try {
				Thread.sleep(5000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(this.process.isAlive()){
				LOG.info("Forcing stop on port " + this.port);
				this.process.destroyForcibly();
			}
		}
		else{
			LOG.info("Process is null for port " + this.port);
		}
	}

	@Override
	public void run() {
		try {
			start();
		}
		catch (IOException e) {
			e.printStackTrace();
			stop();
		}
	}
}
