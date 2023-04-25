package in.gov.lrit.asp.dnid;

import java.sql.Timestamp;

public class Requestlist {
	String message_id;
	String referencecspId;
	String set_pollcommand;
	String start_pollcommand;
	String imo_number;
	int dnid_no;
	int member_no;
	Timestamp set_sending_timestamp;
	Timestamp start_sending_timestamp;
	Timestamp LritTimestamp;
	boolean set_is_active;
	boolean start_is_active;

	public String getMessage_id() {
		return message_id;
	}

	public void setMessage_id(String message_id) {
		this.message_id = message_id;
	}

	public String getReferencecspId() {
		return referencecspId;
	}

	public void setReferencecspId(String referencecspId) {
		this.referencecspId = referencecspId;
	}

	public String getSet_pollcommand() {
		return set_pollcommand;
	}

	public void setSet_pollcommand(String set_pollcommand) {
		this.set_pollcommand = set_pollcommand;
	}

	public String getStart_pollcommand() {
		return start_pollcommand;
	}

	public void setStart_pollcommand(String start_pollcommand) {
		this.start_pollcommand = start_pollcommand;
	}

	public String getImo_number() {
		return imo_number;
	}

	public void setImo_number(String imo_number) {
		this.imo_number = imo_number;
	}

	public int getDnid_no() {
		return dnid_no;
	}

	public void setDnid_no(int dnid_no) {
		this.dnid_no = dnid_no;
	}

	public int getMember_no() {
		return member_no;
	}

	public void setMember_no(int member_no) {
		this.member_no = member_no;
	}

	public Timestamp getSet_sending_timestamp() {
		return set_sending_timestamp;
	}

	public void setSet_sending_timestamp(Timestamp set_sending_timestamp) {
		this.set_sending_timestamp = set_sending_timestamp;
	}

	public Timestamp getStart_sending_timestamp() {
		return start_sending_timestamp;
	}

	public void setStart_sending_timestamp(Timestamp start_sending_timestamp) {
		this.start_sending_timestamp = start_sending_timestamp;
	}

	public Timestamp getLritTimestamp() {
		return LritTimestamp;
	}

	public void setLritTimestamp(Timestamp lritTimestamp) {
		LritTimestamp = lritTimestamp;
	}

	public boolean isSet_is_active() {
		return set_is_active;
	}

	public void setSet_is_active(boolean set_is_active) {
		this.set_is_active = set_is_active;
	}

	public boolean isStart_is_active() {
		return start_is_active;
	}

	public void setStart_is_active(boolean start_is_active) {
		this.start_is_active = start_is_active;
	}

}
