package in.gov.lrit.asp.exception;

public class ReportProcessException extends Exception {
	
	String msg;
	public ReportProcessException(String msg) {
		
		this.msg = msg;
	}
		public String getMsg() {
		return msg;
	}
}
