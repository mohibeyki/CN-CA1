package ir.salazar.university;

import java.net.Socket;

public class ClientData {
	private String name;
	private Socket socket;
	private int id;
	private Thread thread;
	private boolean isAlive;
	private String ip;

	public ClientData(Socket socket) {
		this.name = "";
		this.socket = socket;
		this.id = -1;
		this.thread = null;
		this.isAlive = true;
		this.ip = "";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Thread getThread() {
		return thread;
	}

	public void setThread(Thread thread) {
		this.thread = thread;
	}

	public boolean isAlive() {
		return isAlive;
	}

	public void setAlive(boolean isAlive) {
		this.isAlive = isAlive;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
}
