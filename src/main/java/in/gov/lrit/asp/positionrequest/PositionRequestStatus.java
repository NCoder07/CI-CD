package in.gov.lrit.asp.positionrequest;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.imo.gisis.xml.lrit._2008.Response;
import org.imo.gisis.xml.lrit._2008.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import in.gov.lrit.asp.common.DBUpdation;

public class PositionRequestStatus implements Processor {
	


DBUpdation dbProcess;	
public DBUpdation getDbProcess() {
	return dbProcess;
}

public void setDbProcess(DBUpdation dbProcess) {
	this.dbProcess = dbProcess;
}

@Override
public void process(Exchange exchange) {
	Response resp = new Response();
	try {
	Logger log = (Logger) LoggerFactory.getLogger(PositionRequestStatus.class);
	
	log.info("inside my PositionRequestStatus");

	// Step1: log the Request
	
	String message_id = (String) exchange.getIn().getHeader("message_id");	
	
	String request_status = (String) exchange.getIn().getHeader("request_status");
	//dbProcess.updateAspDnidDB(message_id, request_status);
	log.info("[" + message_id + "] Position request completed");
	
	resp.setResponse(ResponseType.SUCCESS);
	dbProcess.updateASPDCMessage(message_id,extractResponse(resp),dbProcess.getCurrentTimestamp());
	exchange.getOut().setBody(resp);
	}
	catch(Exception e)
	{
		System.out.println("Exception: " + e);
		resp.setResponse(ResponseType.SUCCESS);
		exchange.getOut().setBody(resp);
	}
}
 String extractResponse(Response resp) throws JAXBException{
	JAXBContext jxbcr = JAXBContext.newInstance(Response.class);
	Marshaller marshlr = jxbcr.createMarshaller();
	marshlr.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	StringWriter ackBody = new StringWriter();
	marshlr.marshal(resp, ackBody);
	return ackBody.toString();

}

}
