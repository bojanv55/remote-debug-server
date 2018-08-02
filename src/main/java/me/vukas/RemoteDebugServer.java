package me.vukas;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class RemoteDebugServer {
	private static final Logger LOG = Logger.getLogger("me.vukas.RemoteDebugServer");
	private Integer serverPort = 55004;
	private Map.Entry<Integer, Integer> javaPortRange = new AbstractMap.SimpleEntry<>(55005,55015);
	private Map<Integer, DebugProcess> portToProcess = new ConcurrentHashMap<>();
	private Executor executor = Executors.newCachedThreadPool();

	public static void main(String[] args) throws IOException {
		RemoteDebugServer server = new RemoteDebugServer(args);
		server.start();
	}

	private RemoteDebugServer(String[] args) {
		this.parsePorts(args);
	}

	private void parsePorts(String[] args) {
		if (args.length >= 1) {
			this.serverPort = Integer.parseInt(args[0]);
		}
		if(args.length >= 3){
			int start = Integer.parseInt(args[1]);
			int end = Integer.parseInt(args[2]);
			this.javaPortRange = new AbstractMap.SimpleEntry<>(start, end);
		}
	}

	private void start() throws IOException {
		LOG.info("Listening on port " + this.serverPort);
		ServerSocket serverSocket = new ServerSocket(this.serverPort);
		while (true) {
			Socket clientSocket = serverSocket.accept();
			this.executor.execute(new ClientConnection(clientSocket, executor, javaPortRange, portToProcess));
		}
	}
}
