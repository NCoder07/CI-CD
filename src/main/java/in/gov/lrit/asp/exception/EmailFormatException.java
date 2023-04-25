package in.gov.lrit.asp.exception;

public class EmailFormatException extends Exception {
	String msg;
	public EmailFormatException(String msg) {
		// TODO Auto-generated constructor stub
		this.msg=msg;
	}
	
	public String getMsg() {
		return msg;
	}

}
