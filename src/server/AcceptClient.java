package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AcceptClient extends Thread {
	private ServerSocket sc;
	Server server;

	public AcceptClient(ServerSocket sc, Server server) {
		this.sc = sc;
		this.server = server;
	}

	@Override
	public void run() {
		super.run();
		while (true) {
			try {
				Socket clientSocket = sc.accept();
				CommunicateToClient client = new CommunicateToClient(server,
						clientSocket, server.getNumberOfClients());
				client.start();
			} catch (IOException e) {
//				e.printStackTrace();
			}
		}
	}
}
