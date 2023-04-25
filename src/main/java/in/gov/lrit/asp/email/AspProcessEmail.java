//package in.gov.lrit.asp;
package in.gov.lrit.asp.email;

import java.sql.SQLException;
import java.sql.Timestamp;
import javax.xml.bind.JAXBException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.imo.gisis.xml.lrit._2008.Response;
import org.imo.gisis.xml.lrit.positionreport._2008.ShipPositionReportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import in.gov.lrit.asp.common.DBUpdation;
import in.gov.lrit.asp.common.PollCommands;
import in.gov.lrit.asp.exception.EmailFormatException;
import in.gov.lrit.asp.exception.ReportProcessException;

/**
 * <h1>AspProcessEmail.java</h1> This class handles all the incomming emails and
 * processes the pdn's and sends new commands to csp .
 * 
 * @copyright 2019 CDAC Mumbai. All rights reserved
 * @author lrit-team
 * @version 1.0
 */

public class AspProcessEmail implements Processor {
	
	@Autowired
	UnknownReport unknownReport;
	
	@Autowired
	PositionReport positionReport;
	
	@Autowired
	NdnProcess ndnProcess;
	
	@Autowired
	PdnProcess pdnProcess;
	
	@Autowired
	EmailUtilities emailUtilities;
	
	
	DBUpdation dbProcess;

	public DBUpdation getDbProcess() {
		return dbProcess;
	}

	public void setDbProcess(DBUpdation dbProcess) {
		this.dbProcess = dbProcess;
	}

	PollCommands pollCommandType = new PollCommands();
	Response resp = new Response();

	Logger log = (Logger) LoggerFactory.getLogger(AspProcessEmail.class);

	public void process(Exchange arg0) throws EmailFormatException, ReportProcessException, SQLException {

		log.info("INSIDE PROCESSOR");
		Timestamp receivedAtASP = dbProcess.getCurrentTimestamp();
		String email = null;
		email = arg0.getIn().getBody(String.class).trim();
		log.info("Email is =" + email);

		if (email == null) {
			/*
			 * log.error("Email body is Empty"); log.info("Generating fatal Message Error");
			 * // If message body is empty then we are generating fatal Alert.
			 * dbProcess.generateAlert(null, 113);
			 */
			throw new EmailFormatException(" BODY IS EMPTY");
		}
		String subject = (String) arg0.getIn().getHeader("subject");
		String from = (String) arg0.getIn().getHeader("From");
		String timestampAsString = (String) arg0.getIn().getHeader("Date");
		log.info("NEW EMAIL RECEIVED ");
		log.info("BODY OF EMAIL IS --- " + email);
		log.info("SUBJECT OF EMAIL IS --- " + subject);
		log.info("SENDER IS --- " + from);
		log.info("TIMESTAMP IS --- " + timestampAsString);
		String cspEmail = dbProcess.getGolbalProperties("FROM");
		log.info("from and csp " + from + "  " + cspEmail);
		boolean validate = false;

		validate = emailUtilities.emailValidation(from, cspEmail);
		log.info("validate " + validate);
		if (validate == false) {
			log.error("INVALID SOURCE ");
			arg0.getOut().setHeader("choice", "Invalid");
			throw new EmailFormatException("Email NOT FROM VALID SOURCE");
		}
		/*
		 * } catch (Exception e) {
		 * log.error("invalid Source, Email cannot be processed");
		 * log.error(" in Email_validation " + e.getMessage() + " *** " + e.getCause() +
		 * " *** " + e.getStackTrace().toString()); }
		 */

		//boolean flag = false;
		String messageId = null;
		String RDCid;

		RDCid = this.dbProcess.getId("RDC");
		log.info("RDCid = " + RDCid);
		messageId = dbProcess.generateMessageID(RDCid.trim());
		log.info("message id is " + messageId);

		String aspId = this.dbProcess.getId("ASP");
		String cspId = this.dbProcess.getId("CSP");
		log.info("ASPpid =" + aspId);
		String typeOfEmail = null;
		typeOfEmail = emailUtilities.identifyEmailType(email, subject.trim());
		// log.info("Type of EMail:" + typeOfEmail);
		// String NDN = "Negative Delivery notification";
		// String PDN = "Positive Delivery no​tification​";
		String PR = "Maritime Mobile Position Report";
		// String UR = "Unknown Type Position Report";

		/////////////////// Processing of Unknown Email type ///////////////////

		if (typeOfEmail.trim().contains("Unknown")) {

			log.info("EMAIL IS UNKNOWN POSITION REPORT");

			String stringtimestamp2 = null;
			log.info("timestamp in report is" + stringtimestamp2);
			
			
			// function UnknownEmail()
			unknownReport.processUnknownReport(email, subject, cspId, aspId);

		}

		/////////////////// Processing of Position Report ///////////////////

		else if (typeOfEmail.trim().contains(PR.trim())) {
			////// Get details from email to insert Database //////
			try {

				//function processPositionReport()
				ShipPositionReportType shipPosition = positionReport.processPositionReport(receivedAtASP, email, subject, messageId, cspId, RDCid, aspId);
				
				arg0.getOut().setHeader("choice", "PR");
				arg0.getOut().setBody(shipPosition);
				
				try {
					log.info(emailUtilities.marshallPositionReport(shipPosition));
				} catch (JAXBException e) {
					// TODO Auto-generated catch block
					log.error("error in marshelling ");
					throw new EmailFormatException("JAXBException");
				}

				if (arg0.getIn().getBody(Response.class) != null) {
					log.info("response received");
				}
			} catch (Exception e) {
				log.error("Error in Processing Position Report");
				log.error("[" + messageId + "] in  processing_Position_report " + e.getMessage() + " *** "
						+ e.getCause() + " *** " + e.getStackTrace().toString());
			}

		}

		////////////// PROCESS OF NDN //////////////

		else if (typeOfEmail.trim().contains("Negative")) {

			log.info("EMAIL IS NEGATIVE DELIVERY NOTIFICATION");
			
			//function processNdn()
			ndnProcess.processNdn(email, subject);
		}

		////////////////////// PROCESSING OF PDN ///////////////////

		else if (typeOfEmail.trim().contains("Positive")) {
			//function processPdn()
			pdnProcess.processPdn(arg0, email, subject, messageId);
		} else {
			log.error(" EMAIL DOES NOT BELONG TO ANYTYPE ");
			throw new EmailFormatException("UNRECOGNISED EMAIL");

		}
	}
}
