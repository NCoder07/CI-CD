package in.gov.lrit.asp.common;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.imo.gisis.xml.lrit._2008.Response;
import org.imo.gisis.xml.lrit._2008.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import in.gov.lrit.asp.config.MyConfig;
import in.gov.lrit.asp.exception.EmailFormatException;
import in.gov.lrit.asp.exception.ReceiptCodeException;


/**
 * <h1>SendMailToCSP.java</h1>
 * This class handles sending of all root commands which are used in all the ASP-CSP Exchanges.
 * 
 * @copyright 2019 CDAC Mumbai. All rights reserved
 * @author  lrit-team
 * @version 1.0
 */

public class SendMailToCSP implements Processor {
	
	@Autowired
	MyConfig config;
	
	DBUpdation dbProcess;

	public DBUpdation getDbProcess() {
		return dbProcess;
	}

	public void setDbProcess(DBUpdation dbProcess) {
		this.dbProcess = dbProcess;
	}

	PollCommands pollCommandType = new PollCommands();

	static Logger log = (Logger) LoggerFactory.getLogger(SendMailToCSP.class);

	@Override
	public void process(Exchange exchange)
			throws ReceiptCodeException, SQLException, NumberFormatException, DatatypeConfigurationException {
		Response resp = new Response();
		/* try { */
		Logger log = (Logger) LoggerFactory.getLogger(SendMailToCSP.class);

		log.info("INSIDE SEND Mail COMPONENT");
		Timestamp timestamp = dbProcess.getCurrentTimestamp();
		String message_id = (String) exchange.getIn().getHeader("message_id");
		BigInteger messageType = (BigInteger) exchange.getIn().getHeader("MessageType");
		BigInteger requestType = (BigInteger) exchange.getIn().getHeader("requestType");
		int requestTypeInt = requestType.intValue();
		BigInteger accessType = (BigInteger) exchange.getIn().getHeader("accessType");
		int accessTypeInt = accessType.intValue();
		String dnidNo = (String) exchange.getIn().getHeader("dnidNo");
		int memberNo = (int) exchange.getIn().getHeader("memberNo");
		String shipBorneEquipmentId = (String) exchange.getIn().getHeader("shipBorneEquipmentId");
		String imo_no = (String) exchange.getIn().getHeader("imo_no");
		log.info("IMO NUMBER IS " + imo_no);

		log.info("SHIPBORNEEUIPMENTID ID " + shipBorneEquipmentId);
		int receiptcode = 4;

		String commandtypecode = null;
		// boolean validRequestType = true;
		boolean mailRequired = true;
		// Step5: prepare poll command 1. (Download DNID) // Default Values: P1-
		// I,P3 –
		// D, P4 – 0, P6 – 10
		log.info("[" + message_id + "] Inside Send mail CSP. ");
		// log.info("requestType: " + requestTypeInt);
		String modelid=dbProcess.fetchmodelid(shipBorneEquipmentId);
		String modeltype=dbProcess.fetchmodeltype(modelid);
		if (requestTypeInt == 51) // Download
		{
			log.info(" calling prepare download poll command");
			commandtypecode = pollCommandType.prepareDownloadPollCommand(dnidNo, Integer.toString(memberNo),
					shipBorneEquipmentId,modeltype);
			dbProcess.updateAspDnidDB(imo_no,"DNID_DW_TRIGGERED",true);
			
		} else if (requestTypeInt == 56) // DELETE
		{
			commandtypecode = pollCommandType.prepareDeletePollCommand(dnidNo, memberNo + "", shipBorneEquipmentId,modeltype);
			dbProcess.updateAspDnidDB(imo_no,"DNID_DELETE_TRIGGERED",true);
		}
		// If the request type is 1, then ASP will prepare Poll Command 2
		else if (requestTypeInt == 1) {
			log.info("prepare poll command 2: ");
			commandtypecode = pollCommandType.prepareTransmissionPollCommand(dnidNo, shipBorneEquipmentId,modeltype);
		}
		// If the request type is 8 and the access type is 2 then ASP will
		// prepare poll command 5
		else if (requestTypeInt == 8) {
			if (accessTypeInt == 2) {
				commandtypecode = pollCommandType.prepareStopPollCommand(dnidNo, shipBorneEquipmentId, memberNo,modeltype);
			}
		} else if (requestTypeInt == 12) {
			log.info("**********Request Type 12 ");
			String dataUserRequestor = (String) exchange.getIn().getHeader("dataUserRequestor");
			int cgPresent = dbProcess.checkCGPresent(dataUserRequestor, imo_no);
			if (cgPresent == 0) {
				log.info("setting Synchronise");
				exchange.getOut().setHeader("Synchronise", true);
				exchange.getOut().setBody(null);
			} else {
				commandtypecode = pollCommandType.prepareStopPollCommand(dnidNo, shipBorneEquipmentId, memberNo,modeltype);
				int imoPresentEntries = dbProcess.checkImoPresent(imo_no);
				log.info("Number of Imo Entries are" + imoPresentEntries);
				if (imoPresentEntries >= 2) {
					log.info("Terminating Current request");
					dbProcess.terminaterequest(dataUserRequestor, imo_no);
					int minimumfrequency = dbProcess.fetchfrequecyMinimum(imo_no);
					String cg = dbProcess.fetchcgfromTerminal(imo_no, minimumfrequency);

					dbProcess.setstatusTerminal(cg, imo_no, "ACTIVE");

				} else if (imoPresentEntries == 1) {
					XMLGregorianCalendar startTime = (XMLGregorianCalendar) exchange.getIn().getHeader("startTime");
					log.info("Terminating Current request");
					dbProcess.terminaterequest(dataUserRequestor, imo_no);
					String lrit_id = dbProcess.getCgOwnerFromIMONO(imo_no);
					dbProcess.insertTerminalfrequency(message_id, imo_no, dnidNo, memberNo, lrit_id, 360, startTime,
							"ACTIVE");
				}

			}
		}
		// If Request type is in 0,2,3,4,5,6,10 or 11
		else if (requestTypeInt == 0 || requestTypeInt == 2 || requestTypeInt == 3 || requestTypeInt == 4
				|| requestTypeInt == 5 || requestTypeInt == 6 || requestTypeInt == 10 || requestTypeInt == 11) {
			XMLGregorianCalendar startTime = (XMLGregorianCalendar) exchange.getIn().getHeader("startTime");
			String dataUserRequestor = (String) exchange.getIn().getHeader("dataUserRequestor");

			log.info("LOGGING BEFORE CALLING FREQUENCY CALCULATION  ------");
			log.info("IMO NUMBER IS " + imo_no);
			log.info("dataUserRequestor " + dataUserRequestor);
			log.info("requesttypeint " + requestTypeInt);
			log.info("dnid NO " + dnidNo);
			log.info("Messageid" + message_id);
			log.info("member No." + memberNo);
			log.info("startTime " + startTime);

			log.info("request type :::" + requestTypeInt);
			log.info("requestor:" + dataUserRequestor);
			boolean frequencyCheck = true;

			if (requestTypeInt != 0) {

				frequencyCheck = freqencyCalculation(dataUserRequestor, imo_no, requestTypeInt, dnidNo, message_id,
						memberNo, startTime);

				log.info("frequency check " + frequencyCheck);
				if (frequencyCheck == false) {

					commandtypecode = pollCommandType.prepareStopPollCommand(dnidNo, shipBorneEquipmentId, memberNo,modeltype);
				} else if (frequencyCheck == true) {
					log.info("setting Synchronise");
					exchange.getOut().setHeader("Synchronise", frequencyCheck);
					exchange.getOut().setBody(null);
				}
				/*
				 * else if(frequencyCheck==true) { log.error(
				 * "Frequency Synchronised"); throw new EmailFormatException(
				 * "no stop request frequency synchronised"); }
				 */
			}

			else if (requestTypeInt == 0) {
				log.info("dataUserRequestor=" + dataUserRequestor);
				int imoPresentEntries = dbProcess.checkImoPresent(imo_no);
				log.info("Number of Imo Entries are" + imoPresentEntries);
				if (imoPresentEntries >= 2) {
					log.info("Terminating Current request");
					dbProcess.terminaterequest(dataUserRequestor, imo_no);
					int minimumfrequency = dbProcess.fetchfrequecyMinimum(imo_no);
					String cg = dbProcess.fetchcgfromTerminal(imo_no, minimumfrequency);
					dbProcess.setstatusTerminal(cg, imo_no, "ACTIVE");

				} else if (imoPresentEntries == 1) {
					// XMLGregorianCalendar startTime= (XMLGregorianCalendar)
					// exchange.getIn().getHeader("startTime");
					log.info("Terminating Current request");
					dbProcess.terminaterequest(dataUserRequestor, imo_no);
					String lrit_id = dbProcess.getCgOwnerFromIMONO(imo_no);
					dbProcess.insertTerminalfrequency(message_id, imo_no, dnidNo, memberNo, lrit_id, 360, startTime,
							"ACTIVE");
				}

				
				dbProcess.terminateallRequest(imo_no);
				dbProcess.insertTerminalfrequency(message_id, dbProcess.getimo_num(memberNo, Integer.valueOf(dnidNo)),
						dnidNo, memberNo, dataUserRequestor, 360, dbProcess.getCurrentInstance(), "ACTIVE");
				commandtypecode = pollCommandType.prepareStopPollCommand(dnidNo, shipBorneEquipmentId, memberNo,modeltype);
			}
			/*
			 * else { throw new Exception(
			 * "Requestor not valid for this operation"); }
			 */

		}

		log.info("command type code =" + commandtypecode);
		String pollCommand = pollCommandType.getPollCommand();
		log.info("pollCommand: " + pollCommand);
		log.info("++++++++++++++++++++++++");
		// Step6: log and forward the poll to CSP system.
		log.info("[" + message_id + "] Poll Command are: ");
		log.info("[" + pollCommand + "");
		Timestamp timestamp1 = dbProcess.getCurrentTimestamp();
		String dcid = dbProcess.getId("RDC");
		log.info("Commandtype code " + commandtypecode);

		if ((commandtypecode == "10") || (commandtypecode == "11")) {

			log.info("logging in asp_csp Transaction table before sending mail");
			dbProcess.logCspTransaction(null, "ASP", "CSP", timestamp1, pollCommand, Integer.parseInt(dnidNo),
					Integer.parseInt(shipBorneEquipmentId), memberNo, Integer.parseInt(commandtypecode), "null",
					"DNID Commands", false);
			String cspiD = dbProcess.getcspid(Integer.valueOf(dnidNo), Integer.valueOf(shipBorneEquipmentId.trim()));
			dbProcess.insertcspdcmapping(message_id, cspiD);
			try {
				log.info("poll commands from try ::" + pollCommand);
				exchange.getOut().setHeader("from", config.getASP());// dbProcess.getGolbalProperties("ASP")
				exchange.getOut().setHeader("subject", pollCommand);
				exchange.getOut().setHeader("to", config.getCSP()); // lrit.testuser@gmail.comgetGolbalProperties("CSP")
				exchange.getOut().setHeader("request_status", "Success");
				exchange.getOut().setHeader("message_id", message_id);
				exchange.getOut().setHeader("Synchronise", "false");
				exchange.getOut().setBody(null);

				resp.setResponse(ResponseType.SUCCESS);
				// exchange.getOut().setBody(resp);
			} catch (Exception e) {
				log.error("error in sending root email "+e.getMessage()+e.getStackTrace());
			}
		} else {
			dbProcess.logCspTransaction(null, "ASP", "CSP", timestamp1, pollCommand, Integer.parseInt(dnidNo),
					Integer.parseInt(shipBorneEquipmentId), memberNo, Integer.parseInt(commandtypecode), "STOP",
					"poll Command", false);
			String cspiD = dbProcess.getcspid(Integer.valueOf(dnidNo), Integer.valueOf(shipBorneEquipmentId.trim()));
			dbProcess.insertcspdcmapping(message_id, cspiD);
			try {
				log.info("poll commands from try ::" + pollCommand);
				exchange.getOut().setHeader("from", config.getASP());
				exchange.getOut().setHeader("subject", pollCommand);
				exchange.getOut().setHeader("to", config.getCSP()); // lrit.testuser@gmail.com
				exchange.getOut().setHeader("request_status", "Success");
				exchange.getOut().setHeader("message_id", message_id);
				exchange.getOut().setBody(null);

				resp.setResponse(ResponseType.SUCCESS);
				// exchange.getOut().setBody(resp);
			} catch (Exception e) {
				dbProcess.generatereceipt(message_id, messageType, dbProcess.getCurrentTimestamp(), receiptcode, null,
						null);
				throw new ReceiptCodeException(4, dbProcess.generateMessageID("1065"), message_id,
						BigInteger.valueOf(4), "1065", "4005", "Error sending EMail", dbProcess.getCurrentTimestamp(),
						dbProcess.getCurrentDDpVersion());
				// */

			}
		}
	}

	

	

	/////////////// METHOD TO CALCULATE FREQUENCY //////////
	
	boolean freqencyCalculation(String dataUserRequestor, String imo_no, int requestTypeInt, String dnidNo,
			String message_id, int member_no, XMLGregorianCalendar start_time) {
		// if existing position request exists for a Contracting Government
		boolean a = false;

		log.info("[ " + message_id + "] INSIDE FREQUENCY CALCULATION SENDMAIL");
		log.info("[ " + imo_no + "] IMO number  ");
		log.info("[ " + dataUserRequestor + "] data user requestor ");

		try {

			int frequencyRate = 0;
			boolean requestExist = false;
			String status = "ACTIVE";
			log.info("[ " + requestTypeInt + "] Request Type  ");
			if (requestTypeInt == 2) {
				frequencyRate = 15;
				log.info("frequecy rate " + frequencyRate);

			} else if (requestTypeInt == 3) {
				frequencyRate = 30;
				log.info("frequecy rate " + frequencyRate);
			}

			else if (requestTypeInt == 4) {
				frequencyRate = 60;
			} else if (requestTypeInt == 5) {
				frequencyRate = 180;
			} else if (requestTypeInt == 6) {
				frequencyRate = 360;
			} else if (requestTypeInt == 10) {
				frequencyRate = 720;
			} else if (requestTypeInt == 11) {
				frequencyRate = 1440;

			}
			int imoPresentEntries = dbProcess.checkImoPresent(imo_no);
			if (imoPresentEntries == 0) {
				dbProcess.insertTerminalfrequency(message_id, imo_no, dnidNo, member_no, dataUserRequestor,
						frequencyRate, start_time, "ACTIVE");
			} else if (imoPresentEntries == 1) {
				log.info("ONLY ONE ENTRY PRESENT FOR THE IMO");
				String Requestor = dbProcess.fetchcg(Integer.valueOf(imo_no));
				log.info("Requestor is" + Requestor);
				if (Requestor.equals(dataUserRequestor)) {
					log.info("**********************************************************************************");
					dbProcess.terminateallRequest(imo_no);
					dbProcess.insertTerminalfrequency(message_id, imo_no, dnidNo, member_no, dataUserRequestor,
							frequencyRate, start_time, "ACTIVE");

				}
			} else {

				requestExist = dbProcess.checkterminalfrequency(dataUserRequestor, imo_no);
				log.info("Request Exist =" + requestExist);
				if (requestExist == true) {
					String status1 = dbProcess.checkTerminalstatus(dataUserRequestor, imo_no);
					log.info("status=" + status1);
					if (status1.equals("ACTIVE")) {
						log.info("STATUS IS ACTIVE AND REQUEST EXIST =" + requestExist);
						int frequency = dbProcess.getfrequency(Integer.valueOf(dnidNo), member_no);
						String preRequestor = dbProcess.fetchrequestor(imo_no, "ACTIVE");
						if (frequencyRate <= frequency) {

							dbProcess.terminaterequest(dataUserRequestor, imo_no);
							dbProcess.insertTerminalfrequency(message_id, imo_no, dnidNo, member_no, dataUserRequestor,
									frequencyRate, start_time, "ACTIVE");
						} else {
							a = true;
							// dbProcess.terminaterequest(preRequestor,imo_no);
							// dbProcess.insertTerminalfrequency(message_id,imo_no,dnidNo,member_no,dataUserRequestor,frequencyRate,start_time,"ACTIVE");

						}
					}

					else if (!status1.equals("ACTIVE"))

					{
						log.info("STATUS IS INACTIVE AND REQUEST EXIST =" + requestExist);
						log.info("NEW frequency arrived for this IMO and CG > existing for same CG");
						log.info("check if any request exist for other requestor to set ACTIVE");
						// int minfrequency=
						// dbProcess.getminimumfrequency(imo_no,"INACTIVE");
						String preRequestor = dbProcess.fetchrequestor(imo_no, "INACTIVE");
						int frequency = dbProcess.getfrequency(Integer.valueOf(dnidNo), member_no);
						if (frequencyRate < frequency) {
							dbProcess.terminaterequest(preRequestor, imo_no);
							dbProcess.insertTerminalfrequency(message_id, imo_no, dnidNo, member_no, dataUserRequestor,
									frequencyRate, start_time, "ACTIVE");

						} else {
							a = true;
							dbProcess.terminaterequest(preRequestor, imo_no);
							// dbProcess.insertTerminalfrequency(message_id,imo_no,dnidNo,member_no,dataUserRequestor,frequencyRate,start_time,"ACTIVE");
							// dbProcess.setstatusTerminal(preRequestor,imo_no,"ACTIVE");
						}

					}
					// int fre =dbProcess.getminimumfrequency(imo_no,"active");

				} else {
					log.info("REQUEST DOES NOT EXIST FOR THIS CG");
					int minfrequency = dbProcess.getminimumfrequency(imo_no, "ACTIVE");
					log.info("minimum ferquency " + minfrequency);
					if (minfrequency < frequencyRate) {

						dbProcess.insertTerminalfrequency(message_id, imo_no, dnidNo, member_no, dataUserRequestor,
								frequencyRate, start_time, "INACTIVE");
					} else {
						String preRequestor = dbProcess.fetchrequestor(imo_no, "INACTIVE");
						dbProcess.setstatusTerminal(preRequestor, imo_no, "INACTIVE");
						dbProcess.insertTerminalfrequency(message_id, imo_no, dnidNo, member_no, dataUserRequestor,
								frequencyRate, start_time, "ACTIVE");

					}

				}
			}
			

			int delay = dbProcess.getDelay(imo_no);
			Instant instant = getSynctime(timeStampToXMLGregorianCalender(dbProcess.getCurrentTimestamp()),
					frequencyRate, delay);
			Timestamp next = Timestamp.from(instant);
			log.info("next timestamp is" + next);
			dbProcess.setposReqTime(next, message_id);
			dbProcess.setTimeFrequency(Integer.valueOf(dnidNo), member_no, next);

			// if new frequency is less than the enabled frequency
		}

		catch (Exception e) {
			// TODO Auto-generated catch block

			

			log.error("Terminal frequency Error" + e.getMessage() + "  " + e.getStackTrace());
			return a;

		}
		// return next;
		
		return a;
	}

	

	public static Instant getSynctime(XMLGregorianCalendar start_time, int frequencyRate, int delay) {

		log.info("Inside Synchronise Method");
		int Basetime = 1440; // 24hrs
		int A = Basetime - frequencyRate;

		Date start_date = start_time.toGregorianCalendar().getTime();

		Date current_date = new Date();

		log.info("Start Date : " + start_date);
		log.info("Current Date : " + current_date);

		Calendar calendar = Calendar.getInstance();
		if (start_date.compareTo(current_date) <= 0) {
			log.info("Start Time is already lapsed, so set to current time");
			calendar.setTime(current_date);
		} else {
			log.info("Start Time is in future, so set to the start time");
			calendar.setTime(start_date);
		}

		

		int hours = calendar.get(Calendar.HOUR_OF_DAY);
		int minutes = calendar.get(Calendar.MINUTE);

		log.info("Start Time :  Hour : Miniute :  " + hours + ": " + minutes);

		int initialReqTimeMinutes = (hours * 60) + minutes;
		int syncTime = 0;

		log.info("Request Time in minutes : " + initialReqTimeMinutes);
		log.info("A : " + A);
		int B = A - initialReqTimeMinutes;
		log.info("B : " + B);
		if (B > frequencyRate) {
			while (B > frequencyRate) {
				Basetime = A;
				log.info("Modified BaseTime :  " + Basetime);
				A = Basetime - frequencyRate;
				log.info("Modified A : " + A);
				B = A - initialReqTimeMinutes;
				log.info("Modified B : " + B);
			}
		}
		syncTime = A;
		log.info("Calculated SYNC Start Time : " + syncTime);

		syncTime = syncTime + delay;
		log.info("Calculated SYNC Start Time + Delay : " + syncTime);

		if ((syncTime - initialReqTimeMinutes) > frequencyRate + 30) {
			log.info("Difference between sync time & request start time was greater than frequency time + 30 ");
			syncTime = syncTime - frequencyRate;
			log.info("Hence substracting frequencyrate , Now Sync Time : " + syncTime);
		}

		if ((syncTime - initialReqTimeMinutes) < 15) {
			log.info("Difference between sync time & request start time was less than 15 minutes ");
			syncTime = syncTime + frequencyRate;
			log.info("Hence adding frequencyrate , Now Sync Time : " + syncTime);
		}

		while (((syncTime - frequencyRate) - initialReqTimeMinutes) > 15) {
			log.info("Difference between sync time & request time greater than 15, subtract frequency rate");
			syncTime = syncTime - frequencyRate;
		}

		hours = syncTime / 60;
		minutes = syncTime % 60;

		log.info("SYNC Start Time :: Hours:Minutes " + hours + ":" + minutes);
		Calendar newcalendar = Calendar.getInstance();
		newcalendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
		newcalendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
		newcalendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH));
		newcalendar.set(Calendar.HOUR_OF_DAY, hours);
		newcalendar.set(Calendar.MINUTE, minutes);
		newcalendar.set(Calendar.SECOND, 00);
		newcalendar.set(Calendar.MILLISECOND, 00);
		Date d = newcalendar.getTime();

		Instant instant = d.toInstant();
		// System.out.println("Date : " + d.toString());
		log.info("Instant :" + instant);
		return instant;

	}

	public XMLGregorianCalendar timeStampToXMLGregorianCalender(Timestamp ldt) {
		try {
			log.info("Current Time to convert : " + ldt.getTime());
			// DateFormat df = new
			// SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.sssX");
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
			String date = df.format(ldt);
			log.debug("date : " + date);
			XMLGregorianCalendar XGC = DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
			log.debug("Current Time after convertion df format : " + XGC.toGregorianCalendar().getTime());
			return XGC;
		} catch (DatatypeConfigurationException e) {
			log.error("DatatypeConfigurationException"+ e.getMessage()+e.getStackTrace());
			return null;
		}
	}

	

}
