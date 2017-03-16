import java.io.Serializable;
import java.util.Date;

public class Message implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -293738311238270888L;
	public String method;
	public String[] fileName;
	public String repName;
	public Date[] fileDate;
	public boolean[] toBeUpdated; 
	public String user;
	public boolean[] delete;
	
	public Message(String method, String[] fileName, String repName, Date[] fileDate, boolean[] toBeUpdated, String user, boolean[] delete) {
		this.method = method;
		this.fileName = fileName;
		this.repName = repName;
		this.fileDate = fileDate;
		this.toBeUpdated = toBeUpdated;
		this.user = user;
		this.delete = delete;
	}

}
