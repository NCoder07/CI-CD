package in.gov.lrit.asp.positionrequest;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Calendar;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.imo.gisis.xml.lrit.positionrequest._2008.ShipPositionRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import in.gov.lrit.asp.common.DBUpdation;
import in.gov.lrit.asp.exception.ReportProcessException;

public class PositionRequest implements Processor{
	ShipPositionRequestType shipPositionRequestType;

	DBUpdation dbProcess;
	
	public DBUpdation getDbProcess() {
		return dbProcess;
	}

	public void setDbProcess(DBUpdation dbProcess) {
		this.dbProcess = dbProcess;
	}

	Logger log = (Logger) LoggerFactory.getLogger(PositionRequest.class);
	@Override
	public void process(Exchange exchange) throws Exception {
		// TODO Auto-generated method stub
		this.shipPositionRequestType = exchange.getIn().getBody(ShipPositionRequestType.class);
		String message_id = shipPositionRequestType.getMessageId();
		String dataUserRequestor = shipPositionRequestType.getDataUserRequestor();
		String dataUserProvider = shipPositionRequestType.getDataUserProvider();
		XMLGregorianCalendar timeStamp = shipPositionRequestType.getTimeStamp();
		BigInteger messageType = shipPositionRequestType.getMessageType();
		String imoNo = shipPositionRequestType.getIMONum();
		Timestamp time=dbProcess.xmlGregorianCalenderToTimestamp(timeStamp);
		/*boolean updateDb = dbProcess.insertAspDcTxn(message_id, null, dataUserRequestor,
				dataUserProvider, time, messageType, imoNo, "Position Request");
		 exchange.getOut().setBody(shipPositionRequestType);
		 
		 String[] hostAndSender = dbProcess.getIpAddresses(exchange);
			dbProcess.insertASPDCMeassage(message_id,this.marshallPositionRequest(shipPositionRequestType),dbProcess.getCurrentTimestamp(),null,null,hostAndSender[0],hostAndSender[1]);
		*/
		log.info("Position Request Received, Starting Validation");
		boolean validationStatus = true;
		if(!this.validateMessage()) {
			validationStatus =false;
			throw new Exception("INVALID MESSAGE");
		}
		
		    exchange.getOut().setHeader("validationStatus", validationStatus);
		    
			
			//if(imoNo.equals("9663805")) -- local test
			//if(imoNo.equals("1254125") || imoNo.equals("1589647") || imoNo.equals("5963288") || imoNo.equals("5963148") || imoNo.equals("3698521") || imoNo.equals("9663805") )
			if(imoNo.equals("1254125") || imoNo.equals("1589647") || imoNo.equals("5963288") || imoNo.equals("5963148") || imoNo.equals("3698521") || imoNo.equals("9663805") || imoNo.equals("3559968") || imoNo.equals("3695284"))	
				exchange.getOut().setHeader("testvessel", true);
			else{
				boolean updateDb = dbProcess.insertAspDcTxn(message_id, null, dataUserRequestor, dataUserProvider, time, messageType, imoNo, "Position Request");
				String[] hostAndSender = dbProcess.getIpAddresses(exchange);
				dbProcess.insertASPDCMeassage(message_id,this.marshallPositionRequest(shipPositionRequestType),dbProcess.getCurrentTimestamp(),null,null,hostAndSender[0],hostAndSender[1]);
			}
			exchange.getOut().setBody(shipPositionRequestType);
		 
		 log.info("EXITING POSITION REQUEST");
		
	}
	String marshallPositionRequest(ShipPositionRequestType shipPositionRequestType) throws JAXBException {
		JAXBContext jxbc = JAXBContext.newInstance(ShipPositionRequestType.class);
		Marshaller marshl = jxbc.createMarshaller();
		marshl.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		StringWriter xmlBody = new StringWriter();
		org.imo.gisis.xml.lrit.positionrequest._2008.ObjectFactory  of = new org.imo.gisis.xml.lrit.positionrequest._2008.ObjectFactory();
		marshl.marshal(of.createShipPositionRequest(shipPositionRequestType),xmlBody);
		return xmlBody.toString();
	}
 public boolean validateMessage() {
		
		log.info("[ "+shipPositionRequestType .getMessageId()+" ] In ValidateMessage");
		File schemaFile =  new File("ASPShipPositionRequest.xsd");
		Source xmlFile = null;
		try {
			xmlFile = new StreamSource(new StringReader(marshallPositionRequest(shipPositionRequestType)));
		} catch (JAXBException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		SchemaFactory schemaFactory = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	
			/*schema validation against XSD starts*/
			Schema schema = null;
			try {
				
				schema = schemaFactory.newSchema(schemaFile);
				Validator validator = schema.newValidator();
				validator.validate(xmlFile);
				log.info("[ "+shipPositionRequestType .getMessageId()+" ] In ValidateMessage - XSD Validation Passed");
				log.info("**********************************************************VALIDATED***********************************************************************");
				return true;
			} catch (SAXException e) {
				log.error("[" + shipPositionRequestType .getMessageId() + "] in  validateMessage SAXException "+e.getMessage()+" *** "+e.getCause()+" *** "+e.getStackTrace().toString());
				e.printStackTrace();
			} catch (IOException e) {
				log.error("[" + shipPositionRequestType .getMessageId() + "] in  validateMessage IOException "+e.getMessage()+" *** "+e.getCause()+" *** "+e.getStackTrace().toString());
				e.printStackTrace();
			}
			log.info("[ "+shipPositionRequestType.getMessageId()+" ] in validateMessage ends...returns false");
			return false;
	}

}
