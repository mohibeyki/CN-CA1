package ir.salazar.university;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
	public static ArrayList<ClientData> clients;
	public static int totalClients;
	public static boolean isAlive;
	public static Lock lock;

	public static void main(String[] args) {
		Server.clients = new ArrayList<ClientData>();
		Server.totalClients = 0;
		Server.isAlive = true;
		Server.lock = new ReentrantLock();

		ServerSocket serverSocket = null;
		Socket clientSocket = null;
		int id = 1;
		try {
			serverSocket = new ServerSocket(3128);
			while ((clientSocket = serverSocket.accept()) != null) {
				ClientData data = new ClientData("", clientSocket, id++,
						new Thread());
				new ClientConnector(data);
			}
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}

class ClientConnector implements Runnable {

	private ClientData clientData;

	public ClientConnector(ClientData clientData) {
		Server.lock.lock();
		this.clientData = clientData;
		int index = Server.clients.size();
		Server.clients.add(clientData);
		Server.clients.get(index).setThread(
				new Thread(this, "Cli# " + clientData.getId()));
		Server.clients.get(index).getThread().start();
		Server.totalClients++;
		Server.lock.unlock();
		System.out.println("A client has connected!");
		System.out.println("Total connected clients : " + Server.totalClients);
	}

	@Override
	public void run() {
		String line;
		BufferedReader is;
		PrintStream os;
		try {
			is = new BufferedReader(new InputStreamReader(this.clientData
					.getSocket().getInputStream()));
			os = new PrintStream(this.clientData.getSocket().getOutputStream());
			while (!this.clientData.getSocket().isClosed()) {
				line = is.readLine();
				System.out.println("Cli# " + this.clientData.getId() + " : "
						+ line);
				os.println(line);
				if (line.toLowerCase().startsWith("exit")) {
					this.clientData.getSocket().close();
					Server.totalClients--;
					System.out.println("Client with id "
							+ this.clientData.getId() + " has disconnected!");
					System.out.println("Total connected clients : "
							+ Server.totalClients);
					Server.clients.remove(this.clientData);
					if (line.toLowerCase().equals("exitall")) {
						System.out.println("Killing server now...");
						Server.isAlive = false;
						for (ClientData client : Server.clients) {
							client.getSocket().close();
							client.setAlive(false);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
