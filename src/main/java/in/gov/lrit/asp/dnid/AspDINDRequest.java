package in.gov.lrit.asp.dnid;

/*import java.io.StringWriter;*/
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/*import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;*/
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.imo.gisis.xml.lrit._2008.Response;
import org.imo.gisis.xml.lrit._2008.ResponseType;

import org.imo.gisis.xml.lrit.dnidrequest._2008.DNIDRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import in.gov.lrit.asp.common.DBUpdation;
import in.gov.lrit.asp.email.EmailUtilities;

public class AspDINDRequest implements Processor {

	///////////////// THIS CLASS RECEIVES PACKET FROM PORTAL //////////////////////

	
	DBUpdation dbProcess;
	
	public DBUpdation getDbProcess() {
		return dbProcess;
	}

	public void setDbProcess(DBUpdation dbProcess) {
		this.dbProcess = dbProcess;
	}
	
	@Autowired
	EmailUtilities emailUtilities;

	
	DNIDRequestType dnidRequest;
	Logger log = (Logger) LoggerFactory.getLogger(AspDINDRequest.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		Response resp = new Response();

		System.out.println("INSIDE ASPDNIDREQUEST CLASS");

		// Step1: log the Request
		this.dnidRequest = exchange.getIn().getBody(DNIDRequestType.class);
		log.info("[" + dnidRequest.getMessageId() + "] DNID Request Received. ");

		String message_id = dnidRequest.getMessageId();
		BigInteger requestType = dnidRequest.getRequestType();
		// String dnidNo = dnidRequest.getDNIDNo();
		String imoNo = dnidRequest.getIMONum();
		int memberNo = dnidRequest.getMemberNo();
		BigInteger messageType = dnidRequest.getMessageType();
		XMLGregorianCalendar timeStamp = dnidRequest.getTimeStamp();
		BigInteger oceanRegion = dnidRequest.getOceanRegion();
		Timestamp currentTimestamp = new Timestamp(Calendar.getInstance().getTime().getTime());
		log.info("timestamp: " + currentTimestamp);

		boolean validationStatus = (boolean) exchange.getIn().getHeader("validationStatus");
		// Step3: Update Transaction DB
		// get Ship Borne Equipment Id for address code in poll command
		dbProcess.getShipInfo(imoNo);
		String[] data = new String[3];
		data = dbProcess.getShipInfo(imoNo);
		String shipBorneEquipmentId = data[1].trim();
		String dnidNo = data[2].trim();
		log.info("shipid and dnidno is == " + shipBorneEquipmentId + "  " + dnidNo);

		// BigInteger message_type= BigInteger.valueOf(18);

		dbProcess.setDnidInactive(imoNo);
		boolean updateDb = dbProcess.insertAspDninDB(message_id, dnidNo, memberNo, currentTimestamp, oceanRegion,
				imoNo);
		log.info("dbupdatetion successfull" + updateDb);

		//////////////////////////ADDED SYNC CALCULATION //////////////////// 
		int requestTypeInt = requestType.intValue();

		if (requestTypeInt == 51) {
			String lrit_id = dbProcess.getCgOwnerFromIMONO(imoNo);
			int delay = dbProcess.getDelay(imoNo);
			dbProcess.terminateallRequest(imoNo);
			Instant instant = emailUtilities.getSynctime(emailUtilities.timeStampToXMLGregorianCalender(dbProcess.getCurrentTimestamp()), 360, delay);
			Timestamp next = Timestamp.from(instant);
			log.info("next timestamp is" + next);

			dbProcess.insertTerminalfrequency(message_id, dbProcess.getimo_num(memberNo, Integer.valueOf(dnidNo)),
					dnidNo, memberNo, lrit_id, 360,emailUtilities.timeStampToXMLGregorianCalender(next), "INACTIVE");
		}

		// Step3 a: Fail return error to portal
		if (updateDb == false || shipBorneEquipmentId == "")
			validationStatus = false;

		if (validationStatus == false) {
			resp.setResponse(ResponseType.SUCCESS);
			exchange.getOut().setBody(resp);
		}
		// Step3 b: Success return ack to portal
		else {
			resp.setResponse(ResponseType.SUCCESS);
			// Step4: log the DNID request details
			log.info("[" + message_id + "] DNID request details are: ");
			log.info("[MessageId: " + dnidRequest.getMessageId() + "");
			log.info("requestType: " + requestType + "");
			log.info("dnidNo: " + dnidNo + "");
			log.info("imoNo: " + imoNo + "");
			log.info("memberNo: " + memberNo + "");
			log.info("timeStamp: " + timeStamp + "");
			log.info("oceanRegion: " + oceanRegion + "");

			exchange.getOut().setHeader("imo_no", imoNo);
			exchange.getOut().setHeader("message_id", message_id);
			exchange.getOut().setHeader("memberNo", memberNo);
			exchange.getOut().setHeader("requestType", requestType);
			exchange.getOut().setHeader("accessType", requestType);
			exchange.getOut().setHeader("dnidNo", dnidNo);
			exchange.getOut().setHeader("shipBorneEquipmentId", shipBorneEquipmentId);
			exchange.getOut().setHeader("validationStatus", validationStatus);
			exchange.getOut().setHeader("MessageType", messageType);

			exchange.getOut().setBody(resp);
		}

	}

}
