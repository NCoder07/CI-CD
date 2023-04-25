package in.gov.lrit.asp.email;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.imo.gisis.xml.lrit.positionreport._2008.ShipPositionReportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import in.gov.lrit.asp.common.DBUpdation;
import in.gov.lrit.asp.exception.EmailFormatException;

@Component
public class PositionReport {

	@Autowired
	DBUpdation dbProcess;
	
	@Autowired
	EmailUtilities emailUtilities;
	
	Logger log = (Logger) LoggerFactory.getLogger(PositionReport.class);
	
	public ShipPositionReportType processPositionReport(Timestamp receivedAtASP, String email, String subject, String messageId, String cspId, String RDCid, String aspId) throws EmailFormatException, SQLException {
		
		boolean flag = false;
		log.info("EMAIL IS POSITION REPORT");
		String[] lines = email.split("\\r?\\n");
		/* ------------------------------------------------------------ */
		/* Line 1 is ignored because structured changed and NDC-LRIT  
		 * said to ignore parsing of 1st line                                */
		/* ------------------------------------------------------------ */
		//log.info(lines[0]);
		//String[] firstLine = lines[0].split(" ");
		//String stringtimestamp2 = null;
		//log.info("CSP send report to ASP at: " + firstLine[3] + firstLine[4] + firstLine[5]);
		//stringtimestamp2 = firstLine[3] + " " + firstLine[4] + " " + firstLine[5];
		//log.info("Timestamp(Report send from CSP): " + stringtimestamp2);

		String[] secondLine = lines[1].split(":");
		String region = secondLine[3];
		log.info("Region is  " + region);
		String[] forthLine = lines[4].split(",");
		String[] forDNID = forthLine[1].split(":");
		String dnid = forDNID[1].trim();
		log.info("DNID is  " + dnid);
		String[] forMem = forthLine[2].split(":");
		String mem = forMem[1];
		log.info("Member number is  " + mem);
		String[] data = new String[2];
		String[] data1 = new String[3];
		flag = dbProcess.logCspTransaction(null, "CSP", "ASP", dbProcess.getCurrentTimestamp(), subject,
				Integer.valueOf(dnid), 0, Integer.valueOf(mem.trim()), null, email, "Position Report", false);
		if (flag == true) {
			log.info("EMAIL LOGGED SUCCESSFULL");
		}

		dnid = dnid.trim();
		mem = mem.trim();
		//////// fetching ImoNo. and Shipborne Equipment ////////
			data = dbProcess.getImoNumberandshipid(dnid, mem);
			String imoNumber = data[1];
			String shipEquipmentID = data[0];
		
		if (imoNumber == null) {
			log.error("DNID no and Member number does not map to any Imono." + dnid + " " + mem + " "
					+ shipEquipmentID + "    ");
			// dbProcess.generateAlert(dnid, mem, 119);
			throw new EmailFormatException("Error");
		}

		log.info("shipquipmentid and imonumber is ::" + shipEquipmentID + " " + imoNumber);

		/*
		 * String shipStatus=dbProcess.fetchshipstatus(imoNumber);
		 * if(!((shipStatus.equals("SHIP_REGISTERED"))||(shipStatus.equals(
		 * "DNID_DOWNLOADED"))||(shipStatus.equals("SHIP_NOT_REGISTERED")))) { throw new
		 * EmailFormatException("Error"); }
		 */

		String[] sixthLine = lines[6].split(":");
		String[] forPosition = sixthLine[1].split(",");
		String latitude = forPosition[0];
		String longitude = forPosition[1];
		if ((latitude.length() == 0) || (longitude.length() == 0)) {
			log.info("Generating fatal Message Error");
			dbProcess.generateAlert(shipEquipmentID, 113);
			log.error("latitude and longitude not Proper ." + dnid + " " + mem + " " + shipEquipmentID);
			throw new EmailFormatException("Error");
		}
		
		String[] getLat = latitude.split("\\s+");
		Double degree = Double.parseDouble(getLat[1]);
		String direction;
		direction = getLat[3];

		String[] getminute = getLat[2].split("'");
		Double minute = 0.0;

		if (getminute[0] == null || getminute[0].isEmpty())
			log.error("Getting empty minute");
		else
			minute = Double.parseDouble(getminute[0]);
		
		BigDecimal latitudeDecimal = emailUtilities.convertLocation(degree, minute, direction);
		String[] getLong = longitude.split("\\s+");
		Double degreelong = Double.parseDouble(getLong[1]);
		String[] getminutelong = getLong[2].split("'");
		Double minutelong = Double.parseDouble(getminutelong[0]);
		direction = getLong[3];
		BigDecimal longitudeDecimal = emailUtilities.convertLocation(degreelong, minutelong, direction);

		log.info("position is -------  " + latitudeDecimal + "  " + longitudeDecimal);
		String[] eighthLine = lines[8].split(",");
		String[] forSpeed = eighthLine[0].split(":");
		String[] speedvalue = forSpeed[1].split("\\s+");
		if (speedvalue[1].length() == 0) {
			log.info("Generating fatal Message Error");
			dbProcess.generateAlert(shipEquipmentID, 113);
			log.error("Speed is  not Proper in position Report ." + dnid + " " + mem + " " + shipEquipmentID);
			throw new EmailFormatException("Error");
		}
		
		log.info("speed is ::" + speedvalue[1]);
		BigDecimal speed = new BigDecimal(speedvalue[1]);

		String[] forcourse = eighthLine[1].split(":");
		String[] coursevalue = forcourse[1].split("\\s+");
		
		if (coursevalue[1].length() == 0) {
			log.info("Generating fatal Message Alert");
			dbProcess.generateAlert(shipEquipmentID, 113);
			log.error("latitude and longitude not Proper ." + dnid + " " + mem + " " + shipEquipmentID);
			throw new EmailFormatException("Error");
		}
		BigDecimal course = new BigDecimal(coursevalue[1]);

		log.info("speed and course  is  " + speed + " " + course);
		flag = false;
		String[] tenthLine = lines[10].split(":");
		String forTime = null;
		if (tenthLine[1].length() == 0) {
			log.info("Generating spam alert");
			dbProcess.generateAlert(shipEquipmentID, 114);
			log.error("latitude and longitude not Proper ." + dnid + " " + mem + " " + shipEquipmentID);
			throw new EmailFormatException("Error");
		}
		if (tenthLine[2].length() > 1) {
			forTime = tenthLine[1] + ":" + tenthLine[2] + ":00";
		} else
			forTime = tenthLine[1] + ":" + tenthLine[2] + "0" + ":00";
		
		log.info("ship position Time is ::" + forTime);

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy HH:mm:ss");

		Timestamp timeInPositionReport = null;
		try{
			timeInPositionReport = new Timestamp(((java.util.Date) dateFormat.parse(forTime)).getTime());
		}catch(ParseException e){
			log.error("Error in Parsing timestamp");				
		}
		

		String ddpVersionNo = null;
		ddpVersionNo = dbProcess.getCurrentDDpVersion();
		log.info("ddp version is " + ddpVersionNo);

		String aspid = dbProcess.getId("ASP");
		String cspid = dbProcess.getId("CSP");
		String dcid = dbProcess.getId("RDC");
		BigInteger messageType = BigInteger.valueOf(19);
		//BigInteger testing = BigInteger.valueOf(0);
		
		////// Storing in ASP-CSP Transaction Table ////////
		boolean updateDb = dbProcess.insertAspDcTxn(messageId, null, RDCid, aspId,
				dbProcess.getCurrentTimestamp(), messageType, imoNumber, "Position Report");
		if (updateDb == true) {
			log.info("logged in ASP-DC transaction");
		}
		String cspiD = dbProcess.getCspIdNew(Integer.valueOf(dnid), Integer.valueOf(mem.trim()));

		log.debug("cspid from the table is " + cspiD);
		dbProcess.insertcspdcmapping(messageId, cspiD);
		log.debug("Inserted in CSP mapping");

		// Method call to fetch MMSINo., shipname, shipType
		data1 = dbProcess.fetchShipDetails(imoNumber);
		String mmsiNum = data1[0];
		String ship_name = data1[1];
		String ship_type = data1[2];
		log.info("ship name is: " + ship_name);
		log.info("ship type is: " + ship_type);
		String lrit_id = dbProcess.getCgOwnerFromIMONO(imoNumber);
		log.info("lrit_id" + lrit_id);
		if (lrit_id == null) {
			throw new EmailFormatException("Error");
		}
		ShipPositionReportType shipPosition;
		shipPosition = new ShipPositionReportType();
		String[] splitLat = latitude.trim().split(" ");
		for (String line : splitLat) {
			log.debug(line);
		}

		String[] splitLong = longitude.trim().split(" ");
		for (String line : splitLong) {
			log.debug(line);
		}
		String mid1 = EmailUtilities.removeLastCharacter(splitLat[1]);
		String mid2 = EmailUtilities.removeLastCharacter(splitLong[1]);

		log.info("length of longi is" + splitLong[0].length());
		if (splitLong[0].length() == 1) {
			splitLong[0] = "00" + splitLong[0];
		} else if (splitLong[0].length() == 2) {
			splitLong[0] = "0" + splitLong[0];
		}
		if (splitLat[0].length() < 2) {
			splitLat[0] = "0" + splitLat[0];
		}

		log.info("final lat=" + splitLat[0] + "." + mid1 + "." + splitLat[2]);
		String newlat = splitLat[0] + "." + mid1 + "." + splitLat[2];
		String newlong = splitLong[0] + "." + mid2 + "." + splitLong[2];
		log.info("new longitude : " + newlong);
		shipPosition.setLatitude(newlat.trim());
		shipPosition.setLongitude(newlong.trim());

		shipPosition.setSchemaVersion(new BigDecimal((System.getProperty("Schema_Version"))));
		Timestamp timeStamp3 = dbProcess.getCurrentTimestamp();
		log.info("lat : " + latitude + "long: " + longitude);

		flag = dbProcess.logshippositionReport(messageId, newlat, newlong, timeInPositionReport, aspid, cspId,
				shipEquipmentID, receivedAtASP, timeStamp3, dcid, speed, course, Integer.parseInt(imoNumber),
				region, mmsiNum, messageType, lrit_id, lrit_id, ddpVersionNo, ship_type, ship_name, timeStamp3,
				0);

		//latency code
		try {
			
			long milliseconds1 = timeInPositionReport.getTime();
			long milliseconds2 = receivedAtASP.getTime();
			long diff = milliseconds2 - milliseconds1;
			long diffMinutes = diff / (60 * 1000);
			if (diffMinutes > 15) {
				dbProcess.generateAlert(shipEquipmentID, 118);
			}
		} catch (Exception e) {
			log.error("Error in generating alert");
		}
		
		if (flag == true) {
			log.info("POSITION REPORT LOGGED IN DATABASE");
		}

		shipPosition.setTimeStamp1(emailUtilities.timeStampToXMLGregorianCalender(timeInPositionReport));
		shipPosition.setTimeStamp2(emailUtilities.timeStampToXMLGregorianCalender(receivedAtASP));
		shipPosition.setTimeStamp3(emailUtilities.timeStampToXMLGregorianCalender(timeStamp3));
		shipPosition.setDDPVersionNum(ddpVersionNo);
		shipPosition.setShipborneEquipmentId(shipEquipmentID);
		shipPosition.setASPId(aspid);
		shipPosition.setCSPId(cspid);
		shipPosition.setDataUserRequestor(lrit_id);
		shipPosition.setMMSINum(mmsiNum);
		shipPosition.setDCId(dcid);
		shipPosition.setDataUserProvider(lrit_id);
		shipPosition.setReferenceId("");
		shipPosition.setMessageType(messageType);
		shipPosition.setMessageId(messageId);
		shipPosition.setIMONum(imoNumber);
		shipPosition.setResponseType(BigInteger.valueOf(0));
		shipPosition.setShipName(ship_name);
		shipPosition.setTest(new BigInteger(System.getProperty("Test")));
		// getXMLGregorianCalendar(timestamp2)
		shipPosition.setTimeStamp4(emailUtilities.timeStampToXMLGregorianCalender(timeStamp3));
		shipPosition.setTimeStamp5(emailUtilities.timeStampToXMLGregorianCalender(timeStamp3));
		shipPosition.setShipType(ship_type);
		
		boolean check = dbProcess.checkportalstatus(imoNumber);
		log.debug("check is =" + true);
		if (check == true) {
			dbProcess.setportalVesselDetails(imoNumber, "SHIP_REGISTERED", dbProcess.getCurrentTimestamp());
			log.info("Status changed to SHIP_REGISTERED");
			dbProcess.generateAlert(shipEquipmentID, 107);
		}
	
		return shipPosition;

	}

	
}
