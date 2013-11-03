package ir.salazar.university;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

	public static void main(String[] args) {
		Socket clientSocket = null;
		DataOutputStream os = null;
		DataInputStream is = null;
		try {
			clientSocket = new Socket("localhost", 3128);
			os = new DataOutputStream(clientSocket.getOutputStream());
			is = new DataInputStream(clientSocket.getInputStream());
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host: hostname");
		} catch (IOException e) {
			System.err
					.println("Couldn't get I/O for the connection to: hostname");
		}
		if (clientSocket != null && os != null && is != null) {
			try {
				Scanner sc = new Scanner(System.in);
				while (sc.hasNext()) {
					os.writeBytes(sc.nextLine() + "\n");
					String responseLine;
					if ((responseLine = is.readLine()) != null) {
						System.out.println("Server: " + responseLine);
						if (responseLine.indexOf("Ok") != -1) {
							break;
						}
					}
				}
				sc.close();
				os.close();
				is.close();
				clientSocket.close();
			} catch (UnknownHostException e) {
				System.err.println("Trying to connect to unknown host: " + e);
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
			}
		}
	}
}
