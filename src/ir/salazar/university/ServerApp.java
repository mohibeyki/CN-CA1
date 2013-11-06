package ir.salazar.university;

import java.io.*;
import java.net.*;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.StringTokenizer;
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
		try {
			System.out.println("Server is running and kickin on port 2013");
			ServerApp.serverSocket = new ServerSocket(2013);
			while ((clientSocket = ServerApp.serverSocket.accept()) != null) {
				ServerApp.clients.add(new ClientData(clientSocket));
				new ClientHandler(ServerApp.clients.size() - 1);
			}
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}

class ClientHandler implements Runnable {

	private ClientData clientData;
	String line;
//	BufferedReader is;
//	BufferedInputStream is;
	InputStream in;
	OutputStream os;

	public ClientHandler(int index) {
		ServerApp.lock.lock();
		this.clientData = ServerApp.clients.get(index);

		try {
//			is = new BufferedReader(new InputStreamReader(this.clientData
//					.getSocket().getInputStream()));
			in = clientData.getSocket().getInputStream();
			os = new PrintStream(clientData.getSocket().getOutputStream());
			byte[] buffer = new byte[255];
			int count = in.read(buffer);
			line = new String(buffer, 0,count);
			if (line.length() > 0) {
				// clientData.setName(line);
				os.write(line.getBytes());
			} else {
				os.write("ERROR".getBytes());
				closeClient();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(clientData.getSocket().getRemoteSocketAddress()
				.toString());

		clientData.setThread(new Thread(this, "Cli# " + clientData.getId()));
		clientData.getThread().start();
		ServerApp.totalClients++;
		ServerApp.lock.unlock();
	}

	public boolean isRegistered() {
		return clientData.getName().equals("") == false;
	}

	@Override
	public void run() {
		try {
			System.out.println("\"" + line + "\"" + " has connected!");
			System.out.println("Total connected clients : "
					+ ServerApp.totalClients);
			while (!this.clientData.getSocket().isClosed()) {
				byte[] buffer = new byte[255];
				int count = in.read(buffer);
				line = new String(buffer, 0,count);
								
				System.out.println("Cli# " + this.clientData.getId() + " : "
						+ line);

				Job job = null;
				StringTokenizer st = new StringTokenizer(line);
				String s = st.nextToken();
				if (s.toLowerCase().equals("register")) {
					if (!registerUser(st))
						continue;
				} else if (s.toLowerCase().equals("save")) {
					if (!saveFile(st))
						continue;
				} else if (line.toLowerCase().startsWith("share")) {
					if (isRegistered() == false) {
						os.write("ERR: You are not registered yet.".getBytes());
						continue;
					}
					String userName, fileName;
					if (st.countTokens() != 2) {
						os.write("ERR: Invalid command.".getBytes());
						continue;
					}
					userName = st.nextToken();
					fileName = st.nextToken();
					if (DBInterface.instance().doesUserHaveThisFile(
							clientData.getName(), fileName) == false) {
						// send client "You don't have this file."
						os.write("ERR: You don't have this file.".getBytes());
						continue;
					}
					if (DBInterface.instance().isValidUserFile(userName,
							fileName)) {
						// send client "desired user already has this file."
						os.write("ERR: Your destination already has this file.".getBytes());
						continue;
					}
					if (DBInterface.instance().doesUserHaveThisFile(userName,
							fileName)
							|| DBInterface.instance().addFile(userName,
									fileName)) {
						// send this file to userName
					} else {
						// send client "This file can't be shared."
						os.write("ERR: This file cannot be shared.".getBytes());
					}
					job = new ShareJob(st.nextToken(), st.nextToken());
				} else if (line.toLowerCase().startsWith("update")) {
					if (isRegistered() == false) {
						// send client "You're not registered yet."
						os.write("ERR: You are not registered yet.".getBytes());
						continue;
					}
					if (st.hasMoreTokens() == false) {
						os.write("ERR: Empty filename.".getBytes());
						continue;
					}
					String fileName = st.nextToken();
					if (DBInterface.instance().isOwner(clientData.getName(),
							fileName) == false) {
						// send client "You are not the owner."
						os.write("ERR: You are not the owner.".getBytes());
						continue;
					}
					if (DBInterface.instance().updateFile(fileName)) {
						// receive file from client
						DBInterface.instance().validateUserFile(
								clientData.getName(), fileName);
						ArrayList<String> users = DBInterface.instance()
								.getUsersWithThisFileName(fileName);
						// send file to these users
						// DBInterface.instance().validateUserFile(users.get(),
						// fileName);
					}

					job = new UpdateJob(st.nextToken());
				}
				if (job != null)
					new ClientJob(job, clientData.getIp());
				exitClient(line);
				killall(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean saveFile(StringTokenizer st) throws IOException {
		String s;
		if (isRegistered() == false) {
			os.write("ERR: You're not registered yet.".getBytes());
			return false;
		}
		if (st.hasMoreTokens() == false) {
			os.write("ERR: Empty name.".getBytes());
			return false;
		}

		s = st.nextToken();
		int index = s.lastIndexOf('/');
		s = s.substring(index < 0 ? 0 : index);
		System.err.println("File name after lastIndex " + s);
		if (DBInterface.instance().addFile(clientData.getName(), s)) {
			System.out.println("You can send your file");
			os.write("Send file".getBytes());
			System.out.println("I told you");
//			InputStream in = null;
//			try {
//				in = clientData.getSocket().getInputStream();
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//				return false;
//			}
			byte[] buffer = new byte[255];
			try {
				System.out.println("Before creating file");
				FileOutputStream fos = new FileOutputStream("server/" + s);
				int status = 0;

				while ((status = in.read(buffer)) > 0) {
					System.out.println("in While " + status + " ");
					if(new String(buffer,0,status).equals("END"))
						break;
					fos.write(buffer,0,status);
					System.out.println("After write");
					
				}
				System.out.println("After reading file");
				// while (!this.clientData.getSocket().isClosed()
				// && (status = is.read()) > 0) {
				// System.out.println("In while " + status);
				// recv = is.read(buffer);
				// System.out.println("recv: " + recv);
				// String qq = new String(buffer);
				// System.out.println("buff: " + qq);
				//
				// os.println(recv);
				// fileBufferedWriter.write(buffer,0,status);
				// if (status == 1)
				// break;
				// }
				fos.close();
			} catch (Exception e) {
				System.err.println("ERR: Cannot receive file");
				e.printStackTrace();
				return false;
			}
			System.out.println("almost End");
			DBInterface.instance().validateUserFile(clientData.getName(), s);
			System.out.println("End");
		} else {
			os.write("ERR: You can't use this filename.".getBytes());
			return false;
		}
		return true;
	}

	private boolean registerUser(StringTokenizer st) throws IOException {
		String s;
		if (isRegistered()) {
			os.write("ERR: You're already registered.".getBytes());
			return false;
		}
		if (st.hasMoreTokens() == false) {
			os.write("ERR: Empty name.".getBytes());
			return false;
		}
		s = st.nextToken();
		if (DBInterface.instance().addUser(s, clientData.getIp())) {
			clientData.setName(s);
			os.write("SUC: You are registered now.".getBytes());
			return true;
		} else {
			os.write("ERR: You can't use this name".getBytes());
		}
		return false;
	}

	private void closeClient() throws IOException {
		this.clientData.getSocket().close();
		ServerApp.totalClients--;
		System.out.println("Client with id " + this.clientData.getId()
				+ " has disconnected!");
		System.out.println("Total connected clients : "
				+ ServerApp.totalClients);
		ServerApp.clients.remove(this.clientData);
	}

	private void killall(String line) throws IOException {
		if (line.toLowerCase().equals("killall")) {
			System.out.println("Killing server now...");
			for (ClientData client : ServerApp.clients)
				client.getSocket().close();
			ServerApp.serverSocket.close();
		}
	}

	private void exitClient(String line) throws IOException {
		if (line.toLowerCase().equals("exit"))
			closeClient();
	}
}

class ClientJob implements Runnable {

	private String destIP;
	private Job job;
	private Thread thread;

	public ClientJob(Job job, String destIP) {
		this.destIP = destIP;
		this.job = job;
		thread = new Thread(this, "ClientJobThread");
		thread.start();
	}

	@Override
	public void run() {
		Socket secondarySocket;
		try {
			secondarySocket = new Socket(destIP, 3128);
			DataOutputStream os = new DataOutputStream(
					secondarySocket.getOutputStream());
			BufferedReader is = new BufferedReader(new InputStreamReader(
					secondarySocket.getInputStream()));
			if (secondarySocket != null && os != null && is != null) {
				if (job.getClass() == RegisterJob.class) {
				} else if (job.getClass() == SaveJob.class) {
				} else if (job.getClass() == ShareJob.class) {
				} else if (job.getClass() == UpdateJob.class) {
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
