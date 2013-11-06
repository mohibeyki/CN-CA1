package ir.salazar.university;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
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
	BufferedReader is;
	PrintStream os;

	public ClientHandler(int index) {
		ServerApp.lock.lock();
		this.clientData = ServerApp.clients.get(index);

		try {
			is = new BufferedReader(new InputStreamReader(this.clientData.getSocket().getInputStream()));
			os = new PrintStream(clientData.getSocket().getOutputStream());
			line = is.readLine();
			if (line.length() > 0) {
				clientData.setName(line);
				os.println(line);
			} else {
				os.println("ERROR");
				closeClient();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(clientData.getSocket().getRemoteSocketAddress().toString());

		clientData.setThread(new Thread(this, "Cli# " + clientData.getId()));
		clientData.getThread().start();
		ServerApp.totalClients++;
		ServerApp.lock.unlock();
	}

	public boolean isRegistered()
	{
		return clientData.getName().equals("") == false;
	}

	@Override
	public void run() {
		try {
			System.out.println("\"" + line + "\"" + " has connected!");
			System.out.println("Total connected clients : " + ServerApp.totalClients);
			while (!this.clientData.getSocket().isClosed()) {
				line = is.readLine();
				System.out.println("Cli# " + this.clientData.getId() + " : " + line);

				Job job = null;
				StringTokenizer st = new StringTokenizer(line);
				String s = st.nextToken();
				if (s.toLowerCase().equals("register")) {
					if (isRegistered() == false) {
						// send client "you're already registered"
						continue;
					}
					s = st.nextToken();
					if (s.equals("")) {
						// send client "Send your nme"
						continue;
					}
					if (DBInterface.instance().addUser(s, clientData.getIp())) {
						// send client "You are registered now"
					} else {
						// send client "You cant use this name"
					}
					// job = new RegisterJob(clientData.getName(),
					// clientData.getIp());
				} else if (s.toLowerCase().equals("save")) {
					if (isRegistered() == false) {
						// send client "You're not registered yet"
						continue;
					}
					s = st.nextToken();
					if (s.equals("") == false && DBInterface.instance().addFile(clientData.getName(), s)) {
						// receive file from client
						DBInterface.instance().validateUserFile(clientData.getName(), s);
					} else {
						// send client "You cant use this filename"
					}
					job = new SaveJob(st.nextToken(), ClientApp.name);
				} else if (line.toLowerCase().startsWith("share")) {
					if (isRegistered() == false) {
						// send client "You're not registered yet"
						continue;
					}
					String userName = st.nextToken();
					String fileName = st.nextToken();
					if(fileName.equals("") == true || userName.equals("") == true)
					{
						//send client "Something's missing"
						continue;
					}
					if (DBInterface.instance().doesUserHaveThisFile(clientData.getName(), fileName) == false) {
						// send client "You don't have this file"
						continue;
					}
					if(DBInterface.instance().isValidUserFile(userName, fileName))
					{
						// send client "desired user already has this file
						continue;
					}
					if ( DBInterface.instance().doesUserHaveThisFile(userName, fileName) || DBInterface.instance().addFile(userName, fileName) ) {
						// send this file to userName
					}
					else
					{
						// send client "This file can't be shared"
					}
					job = new ShareJob(st.nextToken(), st.nextToken());
				} else if (line.toLowerCase().startsWith("update")) {
					if (isRegistered() == false) {
						// send client "You're not registered yet"
						continue;
					}
					String fileName = st.nextToken();
					if(fileName.equals("") == true)
					{
						// send client "FileName is missing"
						continue;
					}
					if( DBInterface.instance().isOwner(clientData.getName(), fileName) == false)
					{
						// send client "You are not the owner"
						continue;
					}
					if(DBInterface.instance().updateFile(fileName))
					{
						//receive file from client
						DBInterface.instance().validateUserFile(clientData.getName(), fileName);
						ArrayList<String> users = DBInterface.instance().getUsersWithThisFileName(fileName);
						//send file to these users
						//DBInterface.instance().validateUserFile(users.get(), fileName);
					}

					job = new UpdateJob(st.nextToken());
				}
				new ClientJob(job, clientData.getIp());
				os.println(line);
				exitClient(line);
				killall(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void closeClient() throws IOException {
		this.clientData.getSocket().close();
		ServerApp.totalClients--;
		System.out.println("Client with id " + this.clientData.getId() + " has disconnected!");
		System.out.println("Total connected clients : " + ServerApp.totalClients);
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
			DataOutputStream os = new DataOutputStream(secondarySocket.getOutputStream());
			BufferedReader is = new BufferedReader(new InputStreamReader(secondarySocket.getInputStream()));
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
