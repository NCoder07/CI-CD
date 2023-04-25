package in.gov.lrit.asp.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h1>PollCommands.java</h1> This class handles Poll Commands which are used in
 * all the ASP-CSP Exchanges.
 * 
 * @copyright 2019 CDAC Mumbai. All rights reserved
 * @author lrit-team
 * @version 1.0
 */

public class PollCommands {

	String pollCommand = "";
	DBUpdation dbProcess;

	public DBUpdation getDbProcess() {
		return dbProcess;
	}

	public void setDbProcess(DBUpdation dbProcess) {
		this.dbProcess = dbProcess;
	}

	public String getPollCommand() {
		return pollCommand;
	}

	public void setPollCommand(String pollCommand) {
		this.pollCommand = pollCommand;
	}

	Logger log = (Logger) LoggerFactory.getLogger(PollCommands.class);

	/**
	 * 
	 * This method Creates Download DNID Poll command
	 * 
	 * @param This method takes DnidNo, Member Number and ShipBorneEquipmentId as
	 *             input.
	 * @return CommandType(10)
	 */

	String prepareDownloadPollCommand(String dnidNo, String memberNumberCode, String shipBorneEquipmentId,
			String modeltype) {

		String pollCommand = "poll ";
		// poll [oceanRegion][Poll Type],[DNID – 5 Digit],[Response Type code],[Sub
		// Address code],[Address code],
		// [Command Type Code],[Member Number code]
		String oceanRegion = System.getProperty("OceanRegion");
		String pollType = System.getProperty("Polltype"); // Individual Poll
		String responseTypecode = System.getProperty("Responsetype"); // Request for a response from on board LRIT
																		// equipments by data reporting
		String subAddresscode = null; // Default value for LRIT application is 0.
		if (modeltype.equals("Type - 1")) {
			subAddresscode = "0";
		} else {
			subAddresscode = "1";
		}
		String addresscode = shipBorneEquipmentId; // 9 digits – Inmarsat C mobile no (When P1 is I) [save
													// shipBorneEquipmentId]
		String commandTypeCode = "10"; // Download DNID.

		pollCommand = pollCommand + oceanRegion + "," + pollType + "," + dnidNo + "," + responseTypecode + ","
				+ subAddresscode + "," + addresscode + "," + commandTypeCode + "," + memberNumberCode;
		log.info("Download DNID poll Command is " + pollCommand);
		setPollCommand(pollCommand);

		return commandTypeCode;
	}

	/**
	 * 
	 * This method Creates Delete DNID Poll command
	 * 
	 * @param This method takes DnidNo, Member Number and ShipBorneEquipmentId as
	 *             input.
	 * @return CommandType(10)
	 */

	String prepareDeletePollCommand(String dnidNo, String memberNumberCode, String shipBorneEquipmentId,
			String modeltype) {

		String pollCommand = "poll ";
		// poll [oceanRegion][Poll Type],[DNID – 5 Digit],[Response Type code],[Sub
		// Address code],[Address code],
		// [Command Type Code],[Member Number code]
		String oceanRegion = System.getProperty("OceanRegion");
		String pollType = System.getProperty("Polltype"); // Individual Poll
		String responseTypecode = System.getProperty("Responsetype"); // Request for a response from on board LRIT
																		// equipments by data reporting
		String subAddresscode = null; // Default value for LRIT application is 0.
		if (modeltype.equals("Type - 1")) {
			subAddresscode = "0";
		} else {
			subAddresscode = "1";
		}
		String addresscode = shipBorneEquipmentId; // 9 digits – Inmarsat C mobile no (When P1 is I) [save
													// shipBorneEquipmentId]
		String commandTypeCode = "11"; // Download DNID.

		pollCommand = pollCommand + oceanRegion + "," + pollType + "," + dnidNo + "," + responseTypecode + ","
				+ subAddresscode + "," + addresscode + "," + commandTypeCode + "," + memberNumberCode;

		log.info("Delete DNID poll Command is " + pollCommand);
		setPollCommand(pollCommand);
		return commandTypeCode;
	}

	/**
	 * 
	 * This method Creates Delete One Time Poll command
	 * 
	 * @param This method takes DnidNo and ShipBorneEquipmentId as input.
	 * @return CommandType(0)
	 */

	public String prepareTransmissionPollCommand(String dnidNo, String shipBorneEquipmentId, String modeltype) {

		// poll command 2
		// Command: < poll Ocean Region,P1,P2,P3,P4,P5,P6 >
		// poll [oceanRegion][Poll Type(p1)],[DNID – 5 Digit(p2)],[Response Type
		// code(p3)],
		// [Sub Address code (p4)],[Address code(p5)], [Command Type Code(p6)],
		String pollCommand = "poll ";

		String oceanRegion = System.getProperty("OceanRegion");
		String pollType = System.getProperty("Polltype"); // Individual Poll
		String responseTypecode = System.getProperty("Responsetype"); // Request for a response from on board LRIT
																		// equipments by data reporting
		String subAddresscode = null; // Default value for LRIT application is 0.
		if (modeltype.equals("Type - 1")) {
			subAddresscode = "0";
		} else {
			subAddresscode = "1";
		}
		String addresscode = shipBorneEquipmentId; // 9 digits – Inmarsat C mobile no (When P1 is I) [save
													// shipBorneEquipmentId]
		String commandTypeCode = "0"; // poll command 2.
		String memberNumberCode = "0";
		pollCommand = pollCommand + oceanRegion + "," + pollType + "," + dnidNo + "," + responseTypecode + ","
				+ subAddresscode + "," + addresscode + "," + commandTypeCode;

		log.info("One time poll Command is " + pollCommand);
		setPollCommand(pollCommand);

		return commandTypeCode;
	}

	/**
	 * 
	 * This method Creates Delete Stop Poll command
	 * 
	 * @param This method takes DnidNo, Member Number and ShipBorneEquipmentId as
	 *             input.
	 * @return CommandType(6)
	 */
	String prepareStopPollCommand(String dnidNo, String shipBorneEquipmentId, int memberNo, String modeltype) {

		// poll command 5
		// Command: < poll Ocean Region,P1,P2,P3,P4,P5,P6 >
		// poll [oceanRegion][Poll Type(p1)],[DNID – 5 Digit(p2)],[Response Type
		// code(p3)],
		// [Sub Address code (p4)],[Address code(p5)], [Command Type Code(p6)],[Member
		// Number code(p7)],
		// [Code for setting the program start(p8)],[Code for setting of the
		// transmission time interval (p9)],
		// [Acknowledgement Code(p10)]
		String pollCommand = "poll ";

		String oceanRegion = System.getProperty("OceanRegion");
		String pollType = System.getProperty("Polltype"); // Individual Poll
		String responseTypecode = System.getProperty("Responsetype"); // Request for a response from on board LRIT
																		// equipments by data reporting
		String subAddresscode = null; // Default value for LRIT application is 0.
		if (modeltype.equals("Type - 1")) {
			subAddresscode = "0";
		} else {
			subAddresscode = "1";
		}
		String addresscode = shipBorneEquipmentId; // 9 digits – Inmarsat C mobile no (When P1 is I) [save
													// shipBorneEquipmentId]
		String commandTypeCode = "6"; // poll command 5.
		int memberNumberCode = memberNo;
		String programStart = "0"; //
		String timeInterval = "10";
		String acknowledgementCode = "1";

		pollCommand = pollCommand + oceanRegion + "," + pollType + "," + dnidNo + "," + responseTypecode + ","
				+ subAddresscode + "," + addresscode + "," + commandTypeCode + "," + memberNumberCode + ","
				+ programStart + "," + timeInterval + "," + acknowledgementCode;
		log.info("Stop poll Command is " + pollCommand);
		setPollCommand(pollCommand);
		return commandTypeCode;

	}

	/**
	 * 
	 * This method Creates Delete Start Poll command
	 * 
	 * @param This method takes DnidNo, Member Number and ShipBorneEquipmentId as
	 *             input.
	 * @return CommandType(5)
	 */

	public String prepareStartPollCommand(String dnidNo, String memberNumberCode, String shipBorneEquipmentId,
			String modeltype) {

		// poll command 4 Start
		// Command: < poll Ocean Region,P1,P2,P3,P4,P5,P6,P7,P8,P9,P10 >
		// poll [oceanRegion][Poll Type(p1)],[DNID – 5 Digit(p2)],[Response Type
		// code(p3)],
		// [Sub Address code (p4)],[Address code(p5)], [Command Type Code(p6)],[Member
		// Number code(p7)],
		// [Code for setting the program start(p8)],[Code for setting of the
		// transmission time interval (p9)],
		// [Acknowledgement Code(p10)]
		String pollCommand = "poll ";

		String oceanRegion = System.getProperty("OceanRegion");
		String pollType = System.getProperty("Polltype"); // Individual Poll
		String responseTypecode = System.getProperty("Responsetype"); // Request for a response from on board LRIT
																		// equipments by data reporting
		String subAddresscode = null;
		if (modeltype.equals("Type - 1")) {
			subAddresscode = "0";
		} else {
			subAddresscode = "1";
		}
		String addresscode = shipBorneEquipmentId; // 9 digits – Inmarsat C mobile no (When P1 is I) [save
													// shipBorneEquipmentId]
		String commandTypeCode = "5"; // poll command 5.
		String memberNumCode = memberNumberCode;
		String programStart = "0"; //
		String timeInterval = "10";
		String acknowledgementCode = "1";

		pollCommand = pollCommand + oceanRegion + "," + pollType + "," + dnidNo + "," + responseTypecode + ","
				+ subAddresscode + "," + addresscode + "," + commandTypeCode + "," + memberNumCode + "," + programStart
				+ "," + timeInterval + "," + acknowledgementCode;

		log.info("Start poll Command is " + pollCommand);
		setPollCommand(pollCommand);
		return commandTypeCode;

	}

	/**
	 * 
	 * This method Creates SET DNID Poll command
	 * 
	 * @param This method takes DnidNo, Member Number, frame Number,Interval and
	 *             ShipBorneEquipmentId as input.
	 * @return CommandType(4)
	 */

	public String prepareSetPollCommand(String dnidNo, String memberNumberCode, String shipBorneEquipmentId,
			String framenumber, String interval, String modeltype) {

		// poll command 4 Start
		// Command: < poll Ocean Region,P1,P2,P3,P4,P5,P6,P7,P8,P9,P10 >
		// poll [oceanRegion][Poll Type(p1)],[DNID – 5 Digit(p2)],[Response Type
		// code(p3)],
		// [Sub Address code (p4)],[Address code(p5)], [Command Type Code(p6)],[Member
		// Number code(p7)],
		// [Code for setting the program start(p8)],[Code for setting of the
		// transmission time interval (p9)],
		// [Acknowledgement Code(p10)]
		String pollCommand = "poll ";

		String oceanRegion = System.getProperty("OceanRegion");
		String pollType = System.getProperty("Polltype"); // Individual Poll
		String responseTypecode = System.getProperty("Responsetype"); // Request for a response from on board LRIT
																		// equipments by data reporting
		String subAddresscode = null; // Default value for LRIT application is 0.
		if (modeltype.equals("Type - 1")) {
			subAddresscode = "0";
		} else {
			subAddresscode = "1";
		}
		String addresscode = shipBorneEquipmentId; // 9 digits – Inmarsat C mobile no (When P1 is I) [save
													// shipBorneEquipmentId]

		String commandTypeCode = "4"; // poll command 4.
		String memberNumCode = memberNumberCode;
		String programStart = framenumber; // FrameNumber
		String timeInterval = interval;
		String acknowledgementCode = "1";

		pollCommand = pollCommand + oceanRegion + "," + pollType + "," + dnidNo + "," + responseTypecode + ","
				+ subAddresscode + "," + addresscode + "," + commandTypeCode + "," + memberNumCode + "," + programStart
				+ "," + timeInterval + "," + acknowledgementCode;

		log.info("Set poll Command is " + pollCommand);
		setPollCommand(pollCommand);
		return commandTypeCode;
	}
}