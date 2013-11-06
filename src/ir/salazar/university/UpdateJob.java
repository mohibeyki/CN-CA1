package ir.salazar.university;

public class UpdateJob implements Job {
	private String fileName;

	public UpdateJob(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
