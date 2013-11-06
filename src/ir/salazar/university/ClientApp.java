package ir.salazar.university;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientApp {

	public static ArrayList<Socket> serverTunnelSockets;
	public static Lock lock;
	public static ServerSocket accepterSocket;
	public static String name = "";

	public static void main(String[] args) {
		ClientApp.serverTunnelSockets = new ArrayList<Socket>();
		ClientApp.lock = new ReentrantLock();
		Socket clientSocket;
		OutputStream os;
		BufferedReader is;
		ClientApp.name = "Jamile";
		try {
			clientSocket = new Socket("localhost", 2013);
			os = clientSocket.getOutputStream();
			is = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			if (clientSocket != null && os != null && is != null)
				new ClientTunnel(clientSocket, os, is);

			System.out
					.println("ServerTunnel in client is running and kickin on port 3128");

			accepterSocket = new ServerSocket(3128);
			Socket tmpSocket = null;
			while ((tmpSocket = accepterSocket.accept()) != null) {
				serverTunnelSockets.add(tmpSocket);
				new ServerTunnel(tmpSocket);
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
//	private PrintStream os;
	private BufferedReader is;
	private OutputStream os;
	Thread thread;

	public ClientTunnel(Socket clientSocket, OutputStream os, BufferedReader is) {
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
			os.write(ClientApp.name.getBytes());
			String responseLine;
			if ((responseLine = is.readLine()) != null)
				if (responseLine.equals(ClientApp.name)) {
					System.out.println("Connected to the host");
					while (sc.hasNext()) {
						String line = sc.nextLine();
						os.write(line.getBytes());
						if ((responseLine = is.readLine()) != null) {
							System.out.println("First Response: "
									+ responseLine);
							StringTokenizer st = new StringTokenizer(line);

							String cmd = st.nextToken();
							if (cmd.equals("register")) {
								System.out.println(responseLine);
							} else if (cmd.equals("save")) {
								String filename = st.nextToken();
								// responseLine = is.readLine();
								System.out.println("Server sent "
										+ responseLine);
								//OutputStream out = clientSocket.getOutputStream();
								BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
								int count = 0;
								byte[] buffer = new byte[255];
								while ((count = in.read(buffer)) > 0) {
									System.out.println("In while");
								     os.write(buffer, 0, count);
								     os.flush();
								     System.out.println("Byte: " + new String(buffer,0,count));
								}
								in.close();
								System.out.println("This is the end");
//								File file = new File(filename);
//								FileInputStream fis = null;
//								BufferedInputStream bis = null;
//								DataInputStream dis = null;
//
//								try {
//									fis = new FileInputStream(file);
//									bis = new BufferedInputStream(fis);
//									dis = new DataInputStream(bis);
//									int sent = 0;
//									while (dis.available() != 0) {
//										System.out.println("In while");
//										byte[] buffer = new byte[255];
//										sent = dis.read(buffer);
//										os.write(sent);
//										System.out.println("sent: " + sent);
//										os.write(buffer);
//										responseLine = is.readLine();
//										// if (!responseLine.equals("" + sent))
//										// {
//										// System.err
//										// .println("ERR: failed to send data correctly");
//										// break;
//										// }
//									}
//									os.write(0);
//									System.out.println("This is the end");
//									fis.close();
//									bis.close();
//									dis.close();
//								} catch (FileNotFoundException e) {
//									System.err.println("ERR: \"" + filename
//											+ "\" " + "file not found");
//								} catch (IOException e) {
//									System.err.println("ERR: IOException");
//								}
							} else if (cmd.equals("share")) {
//								os.println(line);
							} else if (cmd.equals("update")) {

							}

							if (line.toLowerCase().equals("exit")
									|| line.toLowerCase().equals("killall")) {
								for (Socket s : ClientApp.serverTunnelSockets)
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
			ClientApp.accepterSocket.close();
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
		ClientApp.lock.lock();
		this.serverSocket = socket;
		this.thread = new Thread(this, "Client connection to server");
		this.thread.start();
		ClientApp.lock.unlock();
	}

	@Override
	public void run() {
		System.err.println("Running");
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
			ClientApp.serverTunnelSockets.remove(this.serverSocket);
		}
	}
}
