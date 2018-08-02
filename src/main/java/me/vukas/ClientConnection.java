package me.vukas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientConnection implements Runnable {
	private static final Logger LOG = Logger.getLogger("me.vukas.ClientConnection");
	private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;
	private Executor executor;
	private DebugProcess process;
	private Pattern patternStart = Pattern.compile("<START_DEBUG_PROCESS>((?!</START_DEBUG_PROCESS>$).*)</START_DEBUG_PROCESS>");
	private Pattern patternStop = Pattern.compile("<STOP_DEBUG_PROCESS />");
	private Pattern patternPort = Pattern.compile("<START_DEBUG_PROCESS_PORT />");
	private Map.Entry<Integer, Integer> javaPortRange;
	private Map<Integer, DebugProcess> portToProcess;
	private Integer port;

	ClientConnection(Socket clientSocket, Executor executor, Map.Entry<Integer, Integer> javaPortRange, Map<Integer, DebugProcess> portToProcess) throws IOException {
		this.clientSocket = clientSocket;
		this.executor = executor;
		this.javaPortRange = javaPortRange;
		this.portToProcess = portToProcess;
		this.out = new PrintWriter(clientSocket.getOutputStream(), true);
		this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	}

	private void doRun() throws IOException {
		String inputLine;
		while ((inputLine = this.in.readLine()) != null) {
			LOG.info("Received command: " + inputLine);

			Matcher matcherPort = patternPort.matcher(inputLine);
			if (matcherPort.matches()) {
				LOG.info("Reserving port");
				for (int p = javaPortRange.getKey(); p <= javaPortRange.getValue(); p++) {
					if(this.portToProcess.putIfAbsent(p, new DebugProcess(null, null, null)) == null) {
						this.port = p;
						this.out.println("<START_DEBUG_PROCESS_PORT>" + this.port + "</START_DEBUG_PROCESS_PORT>");
						break;
					}
				}
				if (this.port == null) {
					throw new RuntimeException("All ports are being used!");
				}
				LOG.info("Port " + this.port + " reserved");
				continue;
			}

			Matcher matcherStart = patternStart.matcher(inputLine);
			if (matcherStart.matches()) {
				String command = matcherStart.group(1);
				LOG.info("Starting debugging");
				this.process = new DebugProcess(this.out, command, this.port);
				this.portToProcess.put(this.port, this.process);
				executor.execute(this.process);
				continue;
			}

			Matcher matcherStop = patternStop.matcher(inputLine);
			if (matcherStop.matches()) {
				stopDebugging();
				return;
			}
		}
	}

	private void stopDebugging() {
		try {
			LOG.info("Stopping debugging");
			this.process.stop();
			portToProcess.remove(this.port);  //remove connection bound to port
			clientSocket.close();
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			doRun();
		}
		catch (IOException e) {
			e.printStackTrace();
			stopDebugging();
		}
	}
}
