package in.gov.lrit.asp.positionrequest;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.imo.gisis.xml.lrit._2008.Response;
import org.imo.gisis.xml.lrit._2008.ResponseType;


import org.imo.gisis.xml.lrit.positionrequest._2008.ShipPositionRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import in.gov.lrit.asp.common.DBUpdation;

public class AspPositionRequest implements Processor {

	ShipPositionRequestType shipPositionRequestType;

	
	DBUpdation dbProcess;
	public DBUpdation getDbProcess() {
		return dbProcess;
	}

	public void setDbProcess(DBUpdation dbProcess) {
		this.dbProcess = dbProcess;
	}

	public XMLGregorianCalendar timeStampToXMLGregorianCalender(Timestamp ldt) {
		try {
		DateFormat df = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.sssX");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		String date2 = df.format(ldt);
		XMLGregorianCalendar XGC = DatatypeFactory.newInstance().newXMLGregorianCalendar(date2);
		return XGC;
		}catch(DatatypeConfigurationException e) {
			log.error("  in  timestamp_to_XMLgregorian "+e.getMessage()+" *** "+e.getCause()+" *** "+e.getStackTrace().toString());	
		return null;
		}
		}
	Logger log = (Logger) LoggerFactory.getLogger(AspPositionRequest.class);
	
	@Override
	public void process(Exchange exchange) throws Exception {

		Response resp = new Response();
		

		log.info("inside my AspPositionRequest");
		this.shipPositionRequestType = exchange.getIn().getBody(ShipPositionRequestType.class);

		// Step 1: Log Position Rquest From DC
		log.info("[" + shipPositionRequestType.getMessageId() + "] Asp Position Request Received. ");
		
		
		// Step 2: Message Validation
		
		String message_id = shipPositionRequestType.getMessageId();
		//String reference_messageid = "";
		BigInteger messageType = shipPositionRequestType.getMessageType();
		BigInteger accessType = shipPositionRequestType.getAccessType();
		BigInteger requestType = shipPositionRequestType.getRequestType();
		String dataUserRequestor = shipPositionRequestType.getDataUserRequestor();
		String dataUserProvider = shipPositionRequestType.getDataUserProvider();
		XMLGregorianCalendar timeStamp = shipPositionRequestType.getTimeStamp();
		String imoNo = shipPositionRequestType.getIMONum();
		String port =shipPositionRequestType.getPort();
		String portFacilityType =shipPositionRequestType.getPortFacility();
		String  ddpversion = shipPositionRequestType.getDDPVersionNum();
		//BigInteger test = shipPositionRequestType.getTest();
		BigDecimal versionNum= shipPositionRequestType.getSchemaVersion();
		XMLGregorianCalendar startTime=null;
		XMLGregorianCalendar stopTime=null;
		if(requestType.intValue()!=1)
		{
			 startTime = shipPositionRequestType.getRequestDuration().getStartTime();
		     stopTime = shipPositionRequestType.getRequestDuration().getStopTime();
		}
		else{
			 startTime=timeStampToXMLGregorianCalender(dbProcess.getCurrentTimestamp());
	 		 stopTime=timeStampToXMLGregorianCalender(dbProcess.getCurrentTimestamp());
		}
		Timestamp time=dbProcess.xmlGregorianCalenderToTimestamp(timeStamp);
 		int memberNo = 0;
		// Message Validation code
		/* validateMessage();*/
       // log.info("dbrocess =="+dbProcess);
		// update transaction
        
 		
		// get Ship Borne Equipment Id for address code in poll command
		String[] data =  new String[3];
		data=dbProcess.getShipInfo(imoNo);
		String shipBorneEquipmentId = data[1].trim();
		String dnidNo = data[2].trim();
		boolean validationStatus =(boolean)exchange.getIn().getHeader("validationStatus");
		// dbProcess.insertTerminalfrequency(message_id,imoNo,dnidNo,memberNo,dataUserRequestor,240,startTime,"ACTIVE");
		memberNo=Integer.parseInt(data[0].trim());
		 //check if entry exists in position request table
		boolean requestExist =false;
		requestExist= dbProcess.checkPositionRequest(imoNo,shipBorneEquipmentId);
		if(requestExist==true)
		{   String status="INACTIVE";
			dbProcess.setStatusInactive(imoNo,shipBorneEquipmentId,status);
		}
		boolean updatedb = dbProcess.insertPosRequest(message_id,messageType,imoNo,shipBorneEquipmentId,accessType,requestType,timeStamp,startTime,stopTime,"ACTIVE");
		// Step 2a: Message Validation Fail: update transaction return error
		
		if (updatedb == false)
			{validationStatus = false;}
		
		 if (validationStatus == false) 
			 { resp.setResponse(ResponseType.SUCCESS); 
			  exchange.getOut().setBody(resp);}
		 log.info("updatedb="+updatedb+"validationstatus"+validationStatus);
		  
		// Step 2a: Message Validation success: update transaction return ack
		
            try{
            	
            
			resp.setResponse(ResponseType.SUCCESS);
			// Step4: log the DNID request details
			log.info("[" + message_id + "] Asp Position request details are: ");
			log.info("MessageId: " + shipPositionRequestType.getMessageId() + "");
			log.info("requestType: " + requestType + "");
			log.info("dnidNo: " + dnidNo + "");
			log.info("memberNo: " + memberNo + "");
			log.info("timeStamp: " + timeStamp + "");
			log.info("Accesstype: " + accessType+ "");
			log.info("validation status: " +validationStatus+ "");
			log.info("DataUser Requestor :" + dataUserRequestor+ "");
			log.info("DataUser Provider :" + dataUserProvider+ "");
			log.info("starttime :"+startTime);
			log.info("imo_no::"+imoNo);
			
			log.info("SENDING TO MAIL COMPONENT ");

			exchange.getOut().setHeader("message_id", message_id);
			exchange.getOut().setHeader("memberNo", memberNo);
			exchange.getOut().setHeader("requestType", requestType);
			exchange.getOut().setHeader("accessType", accessType);
			exchange.getOut().setHeader("dnidNo", dnidNo);
			exchange.getOut().setHeader("shipBorneEquipmentId", shipBorneEquipmentId);
			exchange.getOut().setHeader("validationStatus", validationStatus);
			exchange.getOut().setHeader("dataUserRequestor", dataUserRequestor);
			exchange.getOut().setHeader("startTime",startTime );
			exchange.getOut().setHeader("imo_no", imoNo);
            }
            catch(Exception e)
            {
            	
            	log.error("[" + message_id + "] in   "+e.getMessage()+" *** "+e.getCause()+" *** "+e.getStackTrace().toString());	
            }
			exchange.getOut().setBody(resp);			

		}

	}

	
	
	

