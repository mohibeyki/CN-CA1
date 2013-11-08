package ir.salazar.university;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.omg.CORBA.RepositoryIdHelper;

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
		InputStream is;
		ClientApp.name = "Jamile";
		try {
			clientSocket = new Socket("localhost", 2013);
			os = clientSocket.getOutputStream();
			is = clientSocket.getInputStream();
			if (clientSocket != null && os != null && is != null)
				new ClientTunnel(clientSocket, os, is);

			System.out.println("ServerTunnel in client is running and kickin on port 3128");

			accepterSocket = new ServerSocket(3128);
			Socket tmpSocket = null;
			while ((tmpSocket = accepterSocket.accept()) != null) {
				serverTunnelSockets.add(tmpSocket);
				new ServerTunnel(tmpSocket);
			}

		} catch (UnknownHostException e) {
			System.err.println("Don't know about host: hostname");
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to: hostname");
		}
	}
}

class ClientTunnel implements Runnable {

	private Socket clientSocket;
	// private PrintStream os;
	private InputStream is;
	private OutputStream os;
	Thread thread;

	public ClientTunnel(Socket clientSocket, OutputStream os, InputStream is) {
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
			byte[] buffer = new byte[1024];
			int count = is.read(buffer);
			responseLine = new String(buffer, 0, count);
			if (responseLine != null)
				if (responseLine.equals(ClientApp.name)) {
					System.out.println("Connected to the host");
					while (sc.hasNext()) {
						String line = sc.nextLine();
						System.out.println("Before write " + line);
						os.write(line.getBytes());
						System.out.println("Before read ");
						count = is.read(buffer);
						System.out.println("After read " + new String(buffer));
						responseLine = new String(buffer, 0, count);
						if (responseLine != null) {
							StringTokenizer st = new StringTokenizer(line);

							String cmd = st.nextToken();
							if (cmd.equals("register")) {
								System.out.println(responseLine);
							} else if (cmd.equals("save")) {
								String filename = st.nextToken();
								int size = (int) (new File(filename)).length();
								size = (int) Math.ceil((double)size / 1024);
								os.write(Integer.toString(size).getBytes());
								os.flush();
								BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
								int count2 = 0;
								byte[] buffer2 = new byte[1024];
								while ((count2 = in.read(buffer2)) > 0) {
									os.write(buffer2, 0, count2);
									os.flush();
								}
								in.close();
								count = is.read(buffer);
								responseLine = new String(buffer, 0, count);
								System.out.println("SUC: " +responseLine);
							} else if (cmd.equals("share")) {
								// os.println(line);
							} else if (cmd.equals("update")) {

							}

							if (line.toLowerCase().equals("exit") || line.toLowerCase().equals("killall")) {
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
		InputStream is;
		OutputStream os;
		try {
			is = serverSocket.getInputStream();
			os = serverSocket.getOutputStream();
			byte[] buffer = new byte[1024];
			int status = is.read(buffer);
			String s = new String(buffer,0,status);
			StringTokenizer st = new StringTokenizer(s);
			String fileName = st.nextToken();
			int size = Integer.parseInt(st.nextToken());
			System.out.println("input file: " + fileName + " " + size);
			os.write("Send file".getBytes());
			try {
				FileOutputStream fos = new FileOutputStream("cli/" + fileName);
				while (size-- > 0 && (status = is.read(buffer)) > 0) {
					fos.write(buffer, 0, status);
				}
				fos.close();
			} catch (Exception e) {
				System.err.println("ERR: Cannot receive file");
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			ClientApp.serverTunnelSockets.remove(this.serverSocket);
		}
	}
}
