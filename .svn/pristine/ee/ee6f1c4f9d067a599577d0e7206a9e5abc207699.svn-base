package in.gov.lrit.asp.exception;

import java.math.BigInteger;
import java.sql.Timestamp;

public class ReceiptCodeException extends Exception{
	
	int MessageType;
	String MessageId;
	String ReferenceId;
	BigInteger ReceiptCode;
	String Destination;
	String Originator;
	String Message;
	Timestamp timeStamp;
	String DDPVersionNum;
	public ReceiptCodeException(int MessageType, String MessageId,String ReferenceId,BigInteger ReceiptCode,String Destination,String Originator,String Message,Timestamp timestamp,String DDPVersionNum  )
	{
		super();
		this.MessageType=MessageType;
		this.MessageId=MessageId;
		this.ReferenceId=ReferenceId;
		this.ReceiptCode=ReceiptCode;
		this.Destination=Destination;
		this.Originator=Originator;
		this.Message=Message;
		this.timeStamp=timestamp;
        this.DDPVersionNum=DDPVersionNum;
        
	}
	public int getMessageType() {
		return MessageType;
	}
	public String getMessageId() {
		return MessageId;
	}
	public String getReferenceId() {
		return ReferenceId;
	}
	public BigInteger  getReceiptCode() {
		return ReceiptCode;
	}
	public String getDestination() {
		return Destination;
	}
	public String getOriginator() {
		return Originator;
	}
	public String getMessage() {
		return Message;
	}
	public Timestamp getTimeStamp() {
		return timeStamp;
	}
	public String getDDPVersionNum() {
		return DDPVersionNum;
	}
	
	

}
