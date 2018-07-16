package me.vukas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteDebugServer {
	private ServerSocket serverSocket;
	private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;
	private DebugProcess p;
	private InputStream is;

	public static void main(String[] args) throws IOException {
		RemoteDebugServer server = new RemoteDebugServer();
		server.start(30506);
	}

	private void start(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		clientSocket = serverSocket.accept();
		out = new PrintWriter(clientSocket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		//send messages
		String inputLine;
		while ((inputLine = in.readLine()) != null) {

			if ("[START]".equals(inputLine)) {
				//new thread -------------


				ExecutorService es = Executors.newSingleThreadExecutor();
				es.execute(() -> {
					try {

						p = new DebugProcess();
						is = p.start();

						System.out.println("Started thread");

						BufferedReader br = new BufferedReader(new InputStreamReader(is));

						String line;
						while((line = br.readLine()) != null){
							out.println(line);
						}

					}
					catch (Exception e){

					}
				});


				///------ new thread
			}

			if ("[STOP]".equals(inputLine)) {
				//find thread---
				System.out.println("STOPPING PROCESS");
				p.stop();
				stop();
				start(30506);
				//------------
			}

		}
	}

	public void stop() throws IOException {
		in.close();
		out.close();
		clientSocket.close();
		serverSocket.close();
	}
}
