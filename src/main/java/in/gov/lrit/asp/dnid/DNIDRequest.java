package in.gov.lrit.asp.dnid;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import org.imo.gisis.xml.lrit.dnidrequest._2008.DNIDRequestType;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;
import in.gov.lrit.asp.common.DBUpdation;


////////////////////////THIS CLASS RECEIVES DNID REQUEST FROM PORTAL,PERFORMS VALIDATION ////////////////// 

public class DNIDRequest implements Processor {

	
	DNIDRequestType dnidRequest;
	
	
    DBUpdation dbProcess;	 
	
	public DBUpdation getDbProcess() {
		return dbProcess;
	}

	public void setDbProcess(DBUpdation dbProcess) {
		this.dbProcess = dbProcess;
	}
    
	Logger log = LoggerFactory.getLogger(DNIDRequest.class);
	
	//////////////////// METHOD TO VALIDATE DNID REQUEST /////////////
	
	public boolean validateMessage() {
		
		log.info("[ "+dnidRequest.getMessageId()+" ] In ValidateMessage");
		File schemaFile =  new File("ASPDNIDRequest.xsd");
		Source xmlFile = null;
		try {
			xmlFile = new StreamSource(new StringReader(marshallDnidRequest(dnidRequest)));
		} catch (JAXBException e1) {
			// TODO Auto-generated catch block
			log.error(" in  in_validate_method_DNID_request "+e1.getMessage()+" *** "+e1.getCause()+" *** "+e1.getStackTrace().toString());
		}
		SchemaFactory schemaFactory = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	
			/*schema validation against XSD starts*/
			Schema schema = null;
			try {
				schema = schemaFactory.newSchema(schemaFile);
				Validator validator = schema.newValidator();
				validator.validate(xmlFile);
				log.info("[ "+dnidRequest.getMessageId()+" ] In ValidateMessage - XSD Validation Passed");
				log.info("**********************************************************VALIDATED***********************************************************************");
				return true;
			} catch (SAXException e) {
				log.error("[" + dnidRequest.getMessageId() + "] in  validateMessage SAXException "+e.getMessage()+" *** "+e.getCause()+" *** "+e.getStackTrace().toString());
				
			} catch (IOException e) {
				log.error("[" + dnidRequest.getMessageId() + "] in  validateMessage IOException "+e.getMessage()+" *** "+e.getCause()+" *** "+e.getStackTrace().toString());
				
			}
			log.info("[ "+dnidRequest.getMessageId()+" ] in validateMessage ends...returns false");
			return false;
	}
	public void process(Exchange exchange) throws Exception {
	//	Response resp = new Response();
		boolean validationStatus =true;
		this.dnidRequest = exchange.getIn().getBody(DNIDRequestType.class);
		String dcid=dbProcess.getId("RDC");
		  String cspid=dbProcess.getId("CSP");
			String message_id = dnidRequest.getMessageId();
			String imoNo = dnidRequest.getIMONum();
			BigInteger messageType = dnidRequest.getMessageType();
		
		boolean flag=  dbProcess.insertAspDcTxn(message_id,null,dcid,cspid,dbProcess.getCurrentTimestamp(),messageType,imoNo,"DNID Request");
		String[] hostAndSender = dbProcess.getIpAddresses(exchange);
		log.info("arr[] ,arr[1]"+hostAndSender);
		dbProcess.insertASPDCMeassage(dnidRequest.getMessageId(),this.marshallDnidRequest(dnidRequest),dbProcess.getCurrentTimestamp(),null,null,hostAndSender[0],hostAndSender[1]);
	
		log.info("DNID Request Received, Starting Validation");
		if(!this.validateMessage()) {
			validationStatus =false;
			throw new Exception("INVALID MESSAGE");
		}
		 
	    exchange.getOut().setHeader("validationStatus", validationStatus);
	    exchange.getOut().setBody(dnidRequest);
		

	}
	String marshallDnidRequest( DNIDRequestType dnidRequest) throws JAXBException {
		
		JAXBContext jxbc = JAXBContext.newInstance(DNIDRequestType.class);
		Marshaller marshl = jxbc.createMarshaller();
		marshl.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		StringWriter xmlBody = new StringWriter();
		org.imo.gisis.xml.lrit.dnidrequest._2008.ObjectFactory  of = new org.imo.gisis.xml.lrit.dnidrequest._2008.ObjectFactory();
		marshl.marshal(of.createDNIDRequest(dnidRequest),xmlBody);
		return xmlBody.toString();
	}
}
