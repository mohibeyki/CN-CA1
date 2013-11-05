package ir.salazar.university;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client {

	public static Lock lock;
	public static ArrayList<Socket> serverTunnelSockets;
	public static ServerSocket accepterSocket = null;
	public static String name = "";

	public static void main(String[] args) {
		Client.serverTunnelSockets = new ArrayList<Socket>();
		Client.lock = new ReentrantLock();
		Socket clientSocket = null;
		PrintStream os = null;
		BufferedReader is = null;
		Client.name = "Jamile";
		try {
			clientSocket = new Socket("localhost", 2013);
			os = new PrintStream(clientSocket.getOutputStream());
			is = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			System.out
					.println("ServerTunnel in client is running and kickin on port 2014");
			if (clientSocket != null && os != null && is != null)
				new ClientTunnel(clientSocket, os, is);

			try {
				accepterSocket = new ServerSocket(2014);
				Socket tmpSocket = null;
				while ((tmpSocket = accepterSocket.accept()) != null) {
					serverTunnelSockets.add(tmpSocket);
					new ServerTunnel(tmpSocket);
				}
			} catch (IOException e) {
				System.out.println(e);
			}

		} catch (UnknownHostException e) {
			System.err.println("Don't know about host: hostname");
		} catch (IOException e) {
			System.err
					.println("Couldn't get I/O for the connection to: hostname");
		}
	}
}

class ClientTunnel implements Runnable {

	private Socket clientSocket;
	private PrintStream os;
	private BufferedReader is;
	Thread thread;

	public ClientTunnel(Socket clientSocket, PrintStream os, BufferedReader is) {
		this.clientSocket = clientSocket;
		this.os = os;
		this.is = is;
		this.thread = new Thread(this, "Client Tunnel");
		this.thread.start();
	}

	@Override
	public void run() {
		try {
			Scanner sc = new Scanner(System.in);
			os.println(Client.name);
			String responseLine;
			if ((responseLine = is.readLine()) != null)
				if (responseLine.equals(Client.name)) {
					System.out.println("Connected to the host");
					while (sc.hasNext()) {
						os.println(sc.nextLine());
						if ((responseLine = is.readLine()) != null) {
							if (!responseLine.equals("exit"))
								System.out.println("Server: " + responseLine);
							if (responseLine.toLowerCase().equals("exit")
									|| responseLine.toLowerCase().equals(
											"killall")) {
								for (Socket s : Client.serverTunnelSockets)
									s.close();
								break;
							}
						}
					}
				} else
					System.err.println("Failed to send my name to host");
			sc.close();
			os.close();
			is.close();
			clientSocket.close();
			Client.accepterSocket.close();
		} catch (UnknownHostException e) {
			System.err.println("Trying to connect to unknown host: " + e);
		} catch (IOException e) {
			System.err.println("IOException: " + e);
		}
	}
}

class ServerTunnel implements Runnable {

	private Thread thread;
	private Socket serverSocket;

	public ServerTunnel(Socket socket) {
		Client.lock.lock();
		this.serverSocket = socket;
		this.thread = new Thread(this, "Client connection to server");
		this.thread.start();
		Client.lock.unlock();
	}

	@Override
	public void run() {
		String line;
		BufferedReader is;
		PrintStream os;
		try {
			is = new BufferedReader(new InputStreamReader(
					this.serverSocket.getInputStream()));
			os = new PrintStream(this.serverSocket.getOutputStream());
			while (!this.serverSocket.isClosed()) {
				line = is.readLine();
				os.println("Server said: " + line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			Client.serverTunnelSockets.remove(this.serverSocket);
		}
	}
}
