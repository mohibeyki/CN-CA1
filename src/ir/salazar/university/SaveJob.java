package ir.salazar.university;

public class SaveJob implements Job {
	private String fileName;
	private String owner;

	public SaveJob(String fileName, String owner) {
		this.fileName = fileName;
		this.owner = owner;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
}
