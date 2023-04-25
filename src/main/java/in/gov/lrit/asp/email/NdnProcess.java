package in.gov.lrit.asp.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import in.gov.lrit.asp.common.DBUpdation;

@Component
public class NdnProcess {

	
	@Autowired
	DBUpdation dbProcess;
	
	@Autowired
	EmailUtilities emailUtilities;
	
	Logger log = (Logger) LoggerFactory.getLogger(NdnProcess.class);

	
	
	public void processNdn(String email, String subject) {
		boolean flag = false;
		String dnidno;
		int mem = 0;
		String[] lines = email.split("\\r?\\n");
		for (String line : lines) {
			log.info(line);
		}
		log.info("lines are" + lines.length);
		String commandtype = null;
		String shipBorneEquipmentId = null;
		String referenceCspId = null;
		boolean flag1 = false;
		String status = "NDN Recceived";
		if (lines.length == 7) {
			String[] SeventhLine = lines[5].split(",");
			dnidno = SeventhLine[2];

			shipBorneEquipmentId = SeventhLine[5];
			mem = dbProcess.fetchmem(shipBorneEquipmentId.trim());
			commandtype = SeventhLine[6].trim();
			log.info("command type in NDN is " + commandtype);

		} else {

			String[] SeventhLine = lines[4].split(",");
			dnidno = SeventhLine[2];

			shipBorneEquipmentId = SeventhLine[5];
			mem = dbProcess.fetchmem(shipBorneEquipmentId.trim());
			commandtype = SeventhLine[6].trim();
			log.info("command type in NDN is " + commandtype);
		}
		//////////// NDN FOR SET COMMAND ////////////
		if (commandtype.equalsIgnoreCase("4")) {
			dbProcess.generateAlert(shipBorneEquipmentId, 104);
			referenceCspId = dbProcess.getcspid(Integer.parseInt(dnidno), Integer.parseInt(shipBorneEquipmentId));

			/// checking if set is against Download DNID
			int rootCommandType = dbProcess.fetchCommandtype(referenceCspId);
			if (rootCommandType == 10) {
				String imo_no = dbProcess.getimo_num(mem, Integer.parseInt(dnidno));

				dbProcess.updateAspDnidDB(imo_no, "SET_FREQ_FAILED", true);
			}

		}

		/////////// NDN FOR ONE TIME POLL ///////////
		if (commandtype.equalsIgnoreCase("0")) {
			commandtype = EmailUtilities.removeLastCharacter(commandtype.trim());
			log.info("last character removed");
			log.info("commandtype=" + commandtype);
			dbProcess.generateAlert(shipBorneEquipmentId, 116);

			flag1 = dbProcess.updateCSPTransaction(referenceCspId, status, Integer.valueOf(commandtype));
			if (flag1 == true) {
				log.info("TRANSACTION UPDATED FOR NDN");
			}

		}
		if (commandtype.equalsIgnoreCase("11")) {

			String imo_no;
			imo_no = dbProcess.getimo_num(mem, Integer.parseInt(dnidno));
			dbProcess.generateAlert(shipBorneEquipmentId, 112);
			dbProcess.setstatusVesselDetails(imo_no, "DNID_DELETE_FAILED", dbProcess.getCurrentTimestamp(),
					"DNID_DELETE_REQ");
			flag1 = dbProcess.updateCSPTransaction(referenceCspId, status, Integer.valueOf(commandtype));
			if (flag1 == true) {
				log.info("TRANSACTION UPDATED For NDN");
			}

		}

		/////////// NDN FOR DOWNLOAD DNID COMMAND ///////////
		if (commandtype.equalsIgnoreCase("10")) {
			dbProcess.generateAlert(shipBorneEquipmentId, 102);

			String imo_no;
			imo_no = dbProcess.getimo_num(mem, Integer.parseInt(dnidno));
			dbProcess.setstatusVesselDetails(imo_no, "DNID_DW_FAILED", dbProcess.getCurrentTimestamp(),
					"DNID_DW_REQ");

			flag1 = dbProcess.updateCSPTransaction(referenceCspId, status, Integer.valueOf(commandtype));
			if (flag1 == true) {
				log.info("TRANSACTION UPDATED For NDN");
			}

		}

		////////// NDN FOR START COMMAND /////////
		if (commandtype.equalsIgnoreCase("5")) {
			dbProcess.generateAlert(shipBorneEquipmentId, 106);
			/// checking if start is against Download DNID
			int rootCommandType = dbProcess.fetchCommandtype(referenceCspId);
			if (rootCommandType == 10) {
				String imo_no = dbProcess.getimo_num(mem, Integer.parseInt(dnidno));

				dbProcess.updateAspDnidDB(imo_no, "START_FAILED", true);
			}
			dbProcess.setStatusTxn(status, referenceCspId, Integer.valueOf(commandtype));

		}

		///////// NDN FOR STOP COMMAND /////////
		if (commandtype.equalsIgnoreCase("6")) {
			dbProcess.generateAlert(shipBorneEquipmentId, 110);
			dbProcess.setStatusTxn(status, referenceCspId, Integer.valueOf(commandtype));

		}

		flag = dbProcess.logCspTransaction(referenceCspId, "CSP", "ASP", dbProcess.getCurrentTimestamp(), subject,
				Integer.valueOf(dnidno), 0, mem, null, email, "Negative Delivery Notification", false);
		if (flag == true) {
			log.info("EMAIL LOGGED SUCCESSFULL");
		}
	}
}
