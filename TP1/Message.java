import java.io.Serializable;

public class Message implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -293738311238270888L;
	public String method;
	public String[] fileName;
	public String[] fileVersion;
	public long[] fileDate;
	public int numFiles;
	//
	public boolean[] toBeUpdated; 
	//public String id;
	//public String rep;
	
	public Message(String method, String[] fileName, String[] fileVersion, long[] fileDate, boolean[] toBeUpdated) {
		this.method = method;
		this.fileName = fileName;
		this.fileVersion = fileVersion;
		this.fileDate = fileDate;
		this.numFiles = fileName.length;
		this.toBeUpdated = toBeUpdated;
	}

}
