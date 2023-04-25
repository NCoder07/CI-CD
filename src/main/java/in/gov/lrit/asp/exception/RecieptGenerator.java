package in.gov.lrit.asp.exception;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.imo.gisis.xml.lrit.receipt._2008.Receipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import in.gov.lrit.asp.common.DBUpdation;



public class RecieptGenerator implements Processor{

	@Autowired
	DBUpdation dbProcess;
	public DBUpdation getDbProcess() {
		return dbProcess;
	}

	public void setDbProcess(DBUpdation dbProcess) {
		this.dbProcess = dbProcess;
	}

	Logger log =  LoggerFactory.getLogger(RecieptGenerator.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		// TODO Auto-generated method stub
		log.info("In ReceiptGeneratorCode "+exchange.getIn().getBody());
		ReceiptCodeException rge= (ReceiptCodeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        log.info("object caught"+rge);
		log.info("exception info:--\n refrenceid = "+rge.getReferenceId()+"\n receipt code = "+rge.getReceiptCode()+"\n destination = "+rge.getDestination()+"\n originator = "+rge.getOriginator()+"\n message = "+rge.getMessage()+"\n MessageID = "+rge.getMessageId());
		if(rge.getReceiptCode()!=null)
		{
		InetAddress addr = InetAddress.getLocalHost();
		String hostip = addr.getHostAddress();
		
		
		
		Receipt receipt=new Receipt();
		/*String messageId=dbProcess.generateMessageID(rge.getOriginator());
		String message=receipt.setMessageId(rge.getMessageId());*/
		receipt.setMessageType(new BigInteger("7"));
		receipt.setMessageId(rge.getMessageId());
		receipt.setReferenceId(rge.getReferenceId());
		receipt.setReceiptCode(rge.getReceiptCode());
		receipt.setDestination(rge.getDestination());
		receipt.setOriginator(rge.getOriginator());
		receipt.setMessage(rge.getMessage());
		receipt.setTimeStamp(dbProcess.getCurrentInstance());
		receipt.setDDPVersionNum(dbProcess.getCurrentDDpVersion());
		receipt.setTest(new BigInteger(System.getProperty("Test")));
		receipt.setSchemaVersion(new BigDecimal(System.getProperty("Schema_Version")));
	
		boolean updateDb = dbProcess.insertAspDcTxn(rge.getMessageId(), null, "3065",
				"40005", dbProcess.getCurrentTimestamp(),BigInteger.valueOf(7), null, "Position Request");
		dbProcess.generatereceipt(rge.getMessageId(),receipt.getMessageType(),dbProcess.getCurrentTimestamp(),rge.getReceiptCode().intValue() ,null,rge.getReferenceId()); 
		log.info("receipt packet"+extractReceipt(receipt));
		dbProcess.insertASPDCMeassage(rge.getMessageId(), extractReceipt(receipt), dbProcess.getCurrentTimestamp(), null, null, null, hostip);
		exchange.getOut().setBody(receipt);
		 
		 //String[] hostAndSender = dbProcess.getIpAddresses(exchange);
			//dbProcess.insertASPDCMeassage(messageId,this.marshallPositionRequest(shipPositionRequestType),dbProcess.getCurrentTimestamp(),null,null,hostAndSender[0],hostAndSender[1]);
		
		}
	}  
	public String extractReceipt(Receipt receipt)
	{
		try{

			JAXBContext jaxbContext = JAXBContext.newInstance(Receipt.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			StringWriter sw = new StringWriter();
			jaxbMarshaller.marshal(receipt, sw);
			return sw.toString();	
		}
		catch(JAXBException jbx)
		{
			log.error("[" + receipt.getMessageId() + "] in extractReceipt JAXBException "+jbx.getMessage()+" *** "+jbx.getCause()+" *** "+jbx.getStackTrace().toString());

		}
		catch(Exception ex)
		{
			log.error("[" + receipt.getMessageId() + "] in  extractReceipt Exception "+ex.getMessage()+" *** "+ex.getCause()+" *** "+ex.getStackTrace().toString());
		}
		log.error("issue in extractPositionRequest marshalling returning null");
		return null;
}
}