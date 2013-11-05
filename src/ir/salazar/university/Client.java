package ir.salazar.university;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

	public static void main(String[] args) {
		Socket clientSocket = null;
		DataOutputStream os = null;
		BufferedReader is = null;
		try {
			clientSocket = new Socket("localhost", 3128);
			os = new DataOutputStream(clientSocket.getOutputStream());
			is = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			ServerThread serverThread = null;
			if (clientSocket != null && os != null && is != null) {
				serverThread = new ServerThread(clientSocket, os, is);
			}
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host: hostname");
		} catch (IOException e) {
			System.err
					.println("Couldn't get I/O for the connection to: hostname");
		}
	}

}

class ServerThread implements Runnable {

	private Socket clientSocket;
	private DataOutputStream os;
	private BufferedReader is;

	public ServerThread(Socket clientSocket, DataOutputStream os,
			BufferedReader is) {
		this.clientSocket = clientSocket;
		this.os = os;
		this.is = is;
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
