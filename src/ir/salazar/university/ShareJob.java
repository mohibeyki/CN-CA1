package ir.salazar.university;

public class ShareJob implements Job {
	private String fileName;
	private String destination;

	public ShareJob(String fileName, String owner) {
		this.fileName = fileName;
		this.destination = owner;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getOwner() {
		return destination;
	}

	public void setOwner(String owner) {
		this.destination = owner;
	}
}
