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
	private Pattern patternPort = Pattern.compile("<START_DEBUG_PROCESS_PORT />");
	private Integer port;

	DebugProcess(PrintWriter out, String command, Integer port) {
		this.out = out;
		this.command = command;
		this.port = port;
	}

	private String commandWithPort(){
		String command = this.command;
		Matcher matcherPort = patternPort.matcher(command);
		if (matcherPort.find()) {
			command = matcherPort.replaceFirst(String.valueOf(this.port));
		}
		return command;
	}

	private void start() throws IOException {
		String command = this.commandWithPort();
		String execCommand = "exec " + command;
		ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", execCommand);
		pb.redirectErrorStream(true);
		LOG.info("Starting process: " + command);
		this.process = pb.start();
		this.writeOutput(this.process.getInputStream());
	}

	private void writeOutput(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = br.readLine()) != null) {
			this.out.println(line);
		}
	}

	void stop() {
		if (this.process != null) {
			LOG.info("Stopping process");
			this.process.destroy();
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
