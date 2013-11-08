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
	// BufferedReader is;
	// BufferedInputStream is;
	InputStream in;
	OutputStream os;

	public ClientHandler(int index) {
		ServerApp.lock.lock();
		this.clientData = ServerApp.clients.get(index);

		try {
			// is = new BufferedReader(new InputStreamReader(this.clientData
			// .getSocket().getInputStream()));
			in = clientData.getSocket().getInputStream();
			os = new PrintStream(clientData.getSocket().getOutputStream());
			byte[] buffer = new byte[255];
			int count = in.read(buffer);
			line = new String(buffer, 0, count);
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

		String tmp = clientData.getSocket().getRemoteSocketAddress().toString();
		clientData.setIp(tmp.substring(1, tmp.lastIndexOf(':')));
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
			System.out.println("Total connected clients : " + ServerApp.totalClients);
			while (!this.clientData.getSocket().isClosed()) {
				System.out.print(">>");
				byte[] buffer = new byte[255];
				int count = in.read(buffer);
				line = new String(buffer, 0, count);

				System.out.println("Cli# " + this.clientData.getIp() + " " + this.clientData.getId() + " : " + line);

				Job job = null;
				StringTokenizer st = new StringTokenizer(line);
				String s = st.nextToken();
				if (s.toLowerCase().equals("register")) {
					if (!registerUser(st))
					{
						System.out.println("Problem in registering.");
						continue;
					}
				} else if (s.toLowerCase().equals("save")) {
					if (!saveFile(st))
					{
						System.out.println("Problem in saving.");
						continue;
					}
				} else if (line.toLowerCase().startsWith("share")) {
					if (!shareFile(st))
					{
						System.out.println("Problem in sharing.");
						continue;
					}
				} else if (line.toLowerCase().startsWith("update")) {
					if(!updateFile(st))
					{
						System.out.println("Problem in updating");
						continue;
					}

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

	private boolean updateFile(StringTokenizer st) throws IOException {
		if (isRegistered() == false) {
			os.write("ERR: You are not registered yet.".getBytes());
			return false;
		}
		if (st.hasMoreTokens() == false) {
			os.write("ERR: Empty filename.".getBytes());
			return false;
		}
		String fileName = st.nextToken();
		if (DBInterface.instance().isOwner(clientData.getName(), fileName) == false) {
			os.write("ERR: You are not the owner.".getBytes());
			return false;
		}
		if (DBInterface.instance().updateFile(fileName)) {
			os.write("Send file".getBytes());
			// TODO: 1024 must be constant
			byte[] buffer = new byte[1024];
			try {
				FileOutputStream fos = new FileOutputStream("server/" + fileName);
				int status = in.read(buffer);
				int size = Integer.parseInt(new String(buffer, 0, status));
				while (size-- > 0 && (status = in.read(buffer)) > 0) {
					fos.write(buffer, 0, status);
				}
				fos.close();
			} catch (Exception e) {
				System.err.println("ERR: Cannot receive file");
				e.printStackTrace();
				return false;
			}
			DBInterface.instance().validateUserFile(clientData.getName(), fileName);
			ArrayList<String> users = DBInterface.instance().getUsersWithThisFileName(fileName);
			String tmpUsers = "";
			for (String userName : users) {
				if(userName == clientData.getName())
					continue;
				new ClientJob(new ShareJob(fileName, userName), DBInterface.instance().getIP(userName));
				DBInterface.instance().validateUserFile(userName,fileName);
				System.out.println("Client Job has been created. " + userName );
				tmpUsers += userName + " ";
			}
			os.write(("SUC: Your file has been shared with " + tmpUsers).getBytes());			
			return true;
			 
		}
		return false;
	}

	private boolean shareFile(StringTokenizer st) throws IOException {
		if (isRegistered() == false) {
			os.write("ERR: You are not registered yet.".getBytes());
			return false;
		}
		String userName, fileName;
		if (st.countTokens() != 2) {
			os.write("ERR: Invalid command.".getBytes());
			return false;
		}
		userName = st.nextToken();
		fileName = st.nextToken();
		if (DBInterface.instance().doesUserHaveThisFile(clientData.getName(), fileName) == false) {
			os.write("ERR: You don't have this file.".getBytes());
			return false;
		}
		if (DBInterface.instance().isValidUserFile(userName, fileName)) {
			os.write("ERR: Your destination already has this file.".getBytes());
			return false;
		}
		if (DBInterface.instance().doesUserHaveThisFile(userName, fileName)
				|| DBInterface.instance().shareFileToUser(userName, fileName)) {
			new ClientJob(new ShareJob(fileName, userName), DBInterface.instance().getIP(userName));
			System.out.println("Client Job has been created.");
			os.write(("SUC: Your file has been shared with " + userName).getBytes());
			return true;
		} else {
			os.write("ERR: This file cannot be shared.".getBytes());
		}
		return false;

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
		if (DBInterface.instance().addFile(clientData.getName(), s)) {
			os.write("Send file".getBytes());
			// TODO: 1024 must be constant
			byte[] buffer = new byte[1024];
			try {
				FileOutputStream fos = new FileOutputStream("server/" + s);
				int status = in.read(buffer);
				int size = Integer.parseInt(new String(buffer, 0, status));
				while (size-- > 0 && (status = in.read(buffer)) > 0) {
					fos.write(buffer, 0, status);
				}
				fos.close();
			} catch (Exception e) {
				System.err.println("ERR: Cannot receive file");
				e.printStackTrace();
				return false;
			}
			DBInterface.instance().validateUserFile(clientData.getName(), s);
		} else {
			os.write("ERR: You can't use this filename.".getBytes());
			return false;
		}
		os.write("Your file has been saved".getBytes());
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
			OutputStream os = secondarySocket.getOutputStream();
			InputStream is = secondarySocket.getInputStream();
			if (secondarySocket != null && os != null) {
				if(job instanceof ShareJob)
				{
					String filename = ((ShareJob) job).getFileName();
					int size = (int) (new File("server/" +filename)).length();
					size = (int) Math.ceil((double)size / 1024);
					System.out.println("Size " + size);
					os.write((filename +" " + Integer.toString(size)).getBytes());
					os.flush();
					BufferedInputStream in = new BufferedInputStream(new FileInputStream("server/" +filename));
					int count2 = 0;
					byte[] buffer2 = new byte[1024];
					count2 = is.read(buffer2);
					System.out.println("Client send: " + new String(buffer2));
					while ((count2 = in.read(buffer2)) > 0) {
						os.write(buffer2, 0, count2);
						os.flush();
					}
					in.close();
					System.out.println("SUC: sending file " + filename + " to " + ((ShareJob) job).getOwner());
				}
				if (job.getClass() == RegisterJob.class) {
				} else if (job.getClass() == SaveJob.class) {
				} else if (job.getClass() == ShareJob.class) {
				} else if (job.getClass() == UpdateJob.class) {
				}
				is.close();
				os.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
