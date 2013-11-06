package ir.salazar.university;

public class RegisterJob implements Job {
	private String username;
	private String ip;

	public RegisterJob(String username, String ip) {
		this.setUsername(username);
		this.setIp(ip);
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
}
