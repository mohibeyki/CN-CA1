package ir.salazar.university;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client {

	public static Lock lock;

	public static void main(String[] args) {
		Client.lock = new ReentrantLock();
		Socket clientSocket = null;
		DataOutputStream os = null;
		BufferedReader is = null;
		try {
			clientSocket = new Socket("localhost", 2013);
			os = new DataOutputStream(clientSocket.getOutputStream());
			is = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			if (clientSocket != null && os != null && is != null)
				new ClientTunnel(clientSocket, os, is);
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
	private DataOutputStream os;
	private BufferedReader is;
	Thread thread;

	public ClientTunnel(Socket clientSocket, DataOutputStream os,
			BufferedReader is) {
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
			while (sc.hasNext()) {
				os.writeBytes(sc.nextLine() + "\n");
				String responseLine;
				if ((responseLine = is.readLine()) != null) {
					System.out.println("Server: " + responseLine);
					if (responseLine.indexOf("Ok") != -1)
						break;
					if (responseLine.toLowerCase().startsWith("exit"))
						break;
				}
			}
			sc.close();
			os.close();
			is.close();
			clientSocket.close();
		} catch (UnknownHostException e) {
			System.err.println("Trying to connect to unknown host: " + e);
		} catch (IOException e) {
			System.err.println("IOException: " + e);
		}
	}
}

class ServerTunnel implements Runnable {

	private Thread thread;
	private Socket socket;

	public ServerTunnel(Socket socket) {
		Client.lock.lock();
		this.socket = socket;
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
					this.socket.getInputStream()));
			os = new PrintStream(this.socket.getOutputStream());
			while (!this.socket.isClosed()) {
				line = is.readLine();
				os.println(line);
				if (line.toLowerCase().startsWith("exit")) {
					this.socket.close();
					if (line.toLowerCase().equals("exitall")) {
						System.out.println("Killing server now...");
						for (ClientData client : Server.clients)
							client.getSocket().close();
						Server.serverSocket.close();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
