package in.gov.lrit.asp.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import in.gov.lrit.asp.common.DBUpdation;

@Component
public class UnknownReport {

	@Autowired
	DBUpdation dbProcess;
	

	Logger log = (Logger) LoggerFactory.getLogger(UnknownReport.class);
	
	public void processUnknownReport(String email, String subject, String cspId, String aspId) {
	
		boolean flag = false;
		
		String[] lines = email.split("\\r?\\n");
		String[] forthLine = lines[4].split(",");
		String[] forDNID = forthLine[1].split(":");
		String dnid = forDNID[1].trim();
		log.info("DNID number is :[" + dnid + "]");
		String[] forMem = forthLine[2].split(":");
		String mem = forMem[1].trim();
		log.info("Member number is :[" + mem + "]");
		flag = dbProcess.logCspTransaction(null, cspId, aspId, dbProcess.getCurrentTimestamp(), subject,
				Integer.valueOf(dnid), 0, Integer.valueOf(mem), null, email, "Unknown Position Report", false);
	
		String[] data = new String[2];
		data = dbProcess.getImoNumberandshipid(dnid.trim(), mem.trim());
		String shipEquipmentID = data[0]; 
	
		log.info("flag=" + flag);
		if (flag == true) {
			log.info("EMAIL LOGGED SUCCESSFULL");
			dbProcess.generateAlert(shipEquipmentID, 120);
		}
		
	}
}
