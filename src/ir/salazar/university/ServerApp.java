package ir.salazar.university;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerApp {
	public static ArrayList<ClientData> clients;
	public static int totalClients;
	public static Lock lock;
	public static ServerSocket serverSocket;

	public static void main(String[] args) {
		ServerApp.clients = new ArrayList<ClientData>();
		ServerApp.totalClients = 0;
		ServerApp.lock = new ReentrantLock();
		ServerApp.serverSocket = null;

		Socket clientSocket = null;
		int id = 1;
		try {
			System.out.println("Server is running and kickin on port 2013");
			ServerApp.serverSocket = new ServerSocket(2013);
			while ((clientSocket = ServerApp.serverSocket.accept()) != null) {
				ClientData data = new ClientData("", clientSocket, id++,
						new Thread());
				new ClientHandler(data);
			}
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}

class ClientHandler implements Runnable {

	private ClientData clientData;
	private int index = -1;

	public ClientHandler(ClientData clientData) {
		ServerApp.lock.lock();
		this.clientData = clientData;
		this.index = ServerApp.clients.size();
		ServerApp.clients.add(clientData);
		ServerApp.clients.get(this.index).setThread(
				new Thread(this, "Cli# " + clientData.getId()));
		ServerApp.clients.get(this.index).getThread().start();
		ServerApp.totalClients++;
		ServerApp.lock.unlock();
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
			line = is.readLine();

			if (line.length() > 0) {
				os.println(line);
				ServerApp.clients.get(index).setName(line);
				System.out.println("\"" + line + "\"" + " has connected!");
				System.out.println("Total connected clients : "
						+ ServerApp.totalClients);
				while (!this.clientData.getSocket().isClosed()) {
					line = is.readLine();
					System.out.println("Cli# " + this.clientData.getId()
							+ " : " + line);
					os.println(line);

					if (line.toLowerCase().equals("exit")) {
						this.clientData.getSocket().close();
						ServerApp.totalClients--;
						System.out.println("Client with id "
								+ this.clientData.getId()
								+ " has disconnected!");
						System.out.println("Total connected clients : "
								+ ServerApp.totalClients);
						ServerApp.clients.remove(this.clientData);
					}

					if (line.toLowerCase().equals("killall")) {
						System.out.println("Killing server now...");
						for (ClientData client : ServerApp.clients)
							client.getSocket().close();
						ServerApp.serverSocket.close();
					}
				}
			} else {
				os.println("ERROR");
				this.clientData.getSocket().close();
				ServerApp.totalClients--;
				System.out.println("Client with id " + this.clientData.getId()
						+ " has disconnected!");
				System.out.println("Total connected clients : "
						+ ServerApp.totalClients);
				ServerApp.clients.remove(this.clientData);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
