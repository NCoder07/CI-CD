package in.gov.lrit.asp.email;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import in.gov.lrit.asp.common.DBUpdation;
import in.gov.lrit.asp.common.PollCommands;

@Component
public class PdnProcess {

	@Autowired
	DBUpdation dbProcess;

	@Autowired
	EmailUtilities emailUtilities;

	Logger log = (Logger) LoggerFactory.getLogger(PdnProcess.class);

	PollCommands pollCommandType = new PollCommands();

	public void processPdn(Exchange arg0, String email, String subject, String messageId) {

		log.info("EMAIL IS POSITIVE DELIVERY NOTIFICATION");
		String[] lines = email.split("\\r?\\n");
		for (String line : lines) {
			log.debug(line);
		}
		String dnidno;
		String shipBorneEquipmentId;
		try {
			String[] fourthLine = lines[4].split(",");
				for (String comma : fourthLine) {
					log.debug(comma);
				}

			dnidno = fourthLine[2].trim();
			shipBorneEquipmentId = fourthLine[5].trim();

			log.info("dnid no. is" + dnidno);
			log.info("shipequipmentID is  " + shipBorneEquipmentId);

			String commandType = null;
			if (fourthLine.length < 8) {
				if (fourthLine[6].trim().length() > 1) {
					char last = fourthLine[6].trim().charAt(1);
					if ((last != 0) || (last != 1)) {
						fourthLine[6] = EmailUtilities.removeLastCharacter(fourthLine[6].trim());
					}
				}
			}
			int commandTypeinMail = Integer.parseInt(fourthLine[6].trim());
			log.info("request Type= " + commandTypeinMail);

			int memNo = dbProcess.fetchmem(shipBorneEquipmentId.trim());
			log.info("memno. is " + memNo);

			String referenceCspId = dbProcess.getcspid(Integer.parseInt(dnidno),
					Integer.parseInt(shipBorneEquipmentId));

			boolean flag = dbProcess.logCspTransaction(referenceCspId, "CSP", "ASP", dbProcess.getCurrentTimestamp(), subject,
					Integer.valueOf(dnidno), 0, memNo, null, email, "Positive Delivery Notification", false);

			if (flag == true) {
				log.info("EMAIL LOGGED SUCCESSFULL");
			}

			log.info("command Type in Mail = " + commandTypeinMail);

			// switch case for switching commandTypeInMail
			// --Neil's new code
			switch (commandTypeinMail) {

			// ONE TIME POLL
			case 0:
				log.info("Generating Alert");
				dbProcess.generateAlert(shipBorneEquipmentId, 115);
				arg0.getOut().setHeader("MAIL", false);
				arg0.getOut().setBody(null);
				break;

			// SET REQUEST
			case 4:
				boolean check = dbProcess.checkStartsent(referenceCspId);
				if (check == true) {
					arg0.getOut().setHeader("MAIL", false);
					arg0.getOut().setBody(null);

				} else {
					int mem = dbProcess.fetchmem(shipBorneEquipmentId.trim());
					log.info("Generate ALert");
					dbProcess.generateAlert(shipBorneEquipmentId, 103);
					String modelid = dbProcess.fetchmodelid(shipBorneEquipmentId);
					String modeltype = dbProcess.fetchmodeltype(modelid);
					commandType = pollCommandType.prepareStartPollCommand(dnidno, String.valueOf(mem),
							shipBorneEquipmentId, modeltype);

					// should be commented because it is logging the START command in DB (START
					// command should be independent)
					boolean flag1 = dbProcess.logCspTransaction(referenceCspId, "ASP", "CSP", dbProcess.getCurrentTimestamp(),
							pollCommandType.getPollCommand(), Integer.valueOf(dnidno), 0, memNo, 5, "START",
							"Start Command", false);
					/////////// code to update status in ASP_DNID_table ////////////
					int rootCommandType = dbProcess.fetchCommandtype(referenceCspId);
					if (rootCommandType == 10) {
						String imo_no = dbProcess.getimo_num(mem, Integer.parseInt(dnidno));

						dbProcess.updateAspDnidDB(imo_no, "START_TRIGGERED", true);
					}
				}
				break;

			// START REQUEST
			case 5:
				boolean flag2 = dbProcess.checkPdnReceived(referenceCspId, commandTypeinMail);
				if (flag2 == false) {
					dbProcess.setStatusTxn("PDN RECEIVED", referenceCspId, commandTypeinMail);
					log.info("Generate Alert");
					dbProcess.generateAlert(shipBorneEquipmentId, 105);
					int rootCommandType = dbProcess.fetchCommandtype(referenceCspId);
					if (rootCommandType == 10) {
						String imo_no = dbProcess.getimo_num(memNo, Integer.parseInt(dnidno));

						dbProcess.updateAspDnidDB(imo_no, "SHIP_CONFIGURED", true);
					}
				}

				arg0.getOut().setHeader("MAIL", false);
				arg0.getOut().setBody(null);

				break;

			// POSITION REQUEST
			case 6:
				boolean flag3 = dbProcess.checkIfStopPDNReceived(dnidno, referenceCspId, memNo);

				if (flag3 == false) {
					int frame = 0;
					dbProcess.generateAlert(shipBorneEquipmentId, 109);
					int requestType = dbProcess.checkrequestType(shipBorneEquipmentId);
					Calendar calendar = Calendar.getInstance();
					if (requestType != 12) {

						boolean flag4 = dbProcess.checkIsSet(dnidno);
						String newMessageID = dbProcess.fetchMessageID(referenceCspId);
						Timestamp startTime = dbProcess.fetchstartTime(newMessageID);
						
						int[] frameUtil = {0,0,0};
						frameUtil = calculateHHMMSS(startTime, calendar);
//						calendar.setTimeInMillis(startTime.getTime());
//						log.info("Start Time will be " + calendar);
//						int HH = calendar.get(Calendar.HOUR_OF_DAY);
//						int mm = calendar.get(Calendar.MINUTE);
//						int ss = calendar.get(Calendar.SECOND);
						frame = emailUtilities.calFrame(frameUtil[0], frameUtil[1], frameUtil[2]);
						
						log.info("Request type is " + requestType);
					} else {
						String imo_no = dbProcess.getimo_num(memNo, Integer.parseInt(dnidno));
						int delay = dbProcess.getDelay(imo_no);
						Instant instant = emailUtilities.getSynctime(
								emailUtilities.timeStampToXMLGregorianCalender(dbProcess.getCurrentTimestamp()), 360,
								delay);
						Timestamp next = Timestamp.from(instant);
						log.info("next timestamp is" + next);

						calendar.setTimeInMillis(next.getTime());
						calendar.add(Calendar.MINUTE, 15);
						int HH = calendar.get(Calendar.HOUR_OF_DAY);
						int mm = calendar.get(Calendar.MINUTE);
						int ss = calendar.get(Calendar.SECOND);
						frame = emailUtilities.calFrame(HH, mm, ss);
						log.info("time is :: " + HH + ":" + mm + ":" + ss);
						log.info("Request type is " + requestType);
					}

					log.info("Request type is " + requestType);
					
					//switch case
					switch (requestType) {
					
					// if ( request type is 12 or 0)
					case 12:
					case 0:
						String imo = dbProcess.getimo_num(memNo, Integer.valueOf(dnidno));
						int frequency = dbProcess.fetchfrequecyMinimum(imo);
						log.info("frequency =" + frequency);
						
								//Switch case for Frequency
								switch (frequency) {
								case 15:
									processRequestForFrequency(frame, memNo, dnidno, shipBorneEquipmentId, referenceCspId, 15,"96");
									break;
									
								case 30:
									processRequestForFrequency(frame, memNo, dnidno, shipBorneEquipmentId, referenceCspId, 30,"48");
									break;
	
								case 60:
									processRequestForFrequency(frame, memNo, dnidno, shipBorneEquipmentId, referenceCspId, 60,"24");
									break;
								
								case 180:
									processRequestForFrequency(frame, memNo, dnidno, shipBorneEquipmentId, referenceCspId, 180,"8");
									break;
									
								case 360:
									processRequestForFrequency(frame, memNo, dnidno, shipBorneEquipmentId, referenceCspId, 360,"4");
									break;
	
								default:
									break;
								}
								// Switch case for Frequency ends here
							
						// break for CASE 12 and CASE 0
						break;
			
					// if ( request type is 8)
					case 8:
						imo = dbProcess.getimo_num(memNo, Integer.valueOf(dnidno));
						dbProcess.terminateallRequest(imo);
						arg0.getOut().setHeader("MAIL", false);
						arg0.getOut().setBody(null);
						break;
					
					// if ( request type is 2)		
					case 2:
						log.info("preparing set poll command request type 2 ");
						processRequest(frame, shipBorneEquipmentId, dnidno, memNo, referenceCspId, "96");

						break;
					
					// if ( request type is 3)	
					case 3:
						log.info("preparing set poll command request type 3 ");
						processRequest(frame, shipBorneEquipmentId, dnidno, memNo, referenceCspId, "48");

						break;
					
					// if ( request type is 4)	
					case 4:
						log.info("preparing set poll command request type 4");
						processRequest(frame, shipBorneEquipmentId, dnidno, memNo, referenceCspId, "24");

						break;
					
					// if ( request type is 5)	
					case 5:
						log.info("preparing set poll command request type 5");
						processRequest(frame, shipBorneEquipmentId, dnidno, memNo, referenceCspId, "8");

						break;
					
					// if ( request type is 6)	
					case 6:
						log.info("preparing set poll command request type 6");
						processRequest(frame, shipBorneEquipmentId, dnidno, memNo, referenceCspId, "4");

						break;
					
					// if ( request type is 10)	
					case 10:
						log.info("preparing set poll command request type 10");
						processRequest(frame, shipBorneEquipmentId, dnidno, memNo, referenceCspId, "2");

						break;
					
					// if ( request type is 11)	
					case 11:
						log.info("preparing set poll command request type 11");
						processRequest(frame, shipBorneEquipmentId, dnidno, memNo, referenceCspId, "1");

						break;
						
					default:
						break;
					}
										
				} else {
					arg0.getOut().setHeader("MAIL", false);
					arg0.getOut().setBody(null);
				}

				break;

			// DOWNLOAD DNID 	
			case 10:

				log.info("command type is " + commandType);
				boolean flag5 = dbProcess.checkDnid(dnidno, "DNID Commands");
				if (flag5 == true) {
					log.info("member number= " + memNo);
					log.info("length= " + fourthLine.length);
					log.info("Generate Alert");

					String imo_no = dbProcess.getimo_num(memNo, Integer.parseInt(dnidno));
					dbProcess.setstatusVesselDetails(imo_no, "DNID_DOWNLOADED", dbProcess.getCurrentTimestamp(),
							"DNID_DW_REQ");
				}

				log.info("dnid no from mail is " + dnidno);
				referenceCspId = dbProcess.getcspid(Integer.parseInt(dnidno), Integer.parseInt(shipBorneEquipmentId));
				log.info("referencecspid" + referenceCspId);
				log.info("shipbornequipment is from mail is" + shipBorneEquipmentId);
				if (dbProcess.checkDownloadPDNReceived(referenceCspId, commandTypeinMail) == false) {
					log.info("First PDN");
					boolean flag6 = dbProcess.updateCSPTransaction(referenceCspId, "PDN RECEIVED", commandTypeinMail);
					if (flag6 == true) {
						log.info("TRANSACTION UPDATED For PDN");
					}
					log.info("status set for command type " + commandTypeinMail);
					dbProcess.generateAlert(shipBorneEquipmentId, 101);
					int mem = dbProcess.fetchmem(shipBorneEquipmentId.trim());
					String imo = dbProcess.getimo_num(mem, Integer.valueOf(dnidno));
					// dbProcess.fetchfrequecyMinimum(imo);
					Calendar calendar = Calendar.getInstance();
					Timestamp time = dbProcess.fetchTimefrequency(imo);
					log.info("time is for this imo" + time);

					/* logic for reducing time by 15 minutes */

					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(time.getTime());
					cal.add(Calendar.MINUTE, -15);
					Timestamp setSendingTime = new Timestamp(cal.getTime().getTime());

					log.info("Sending time is for  " + setSendingTime);// after reducing time by 15 minutes

					String lrit_id = dbProcess.getCgOwnerFromIMONO(imo);
					dbProcess.setstatusTerminal(lrit_id, imo, "ACTIVE");
					calendar.setTimeInMillis(time.getTime());

					/* logic for 7 minutes diff */

					Calendar cal1 = Calendar.getInstance();
					cal1.setTimeInMillis(setSendingTime.getTime());
					cal1.add(Calendar.MINUTE, 7);
					Timestamp startSendingTime = new Timestamp(cal1.getTime().getTime());

					log.info("Sending time is for  " + startSendingTime);//after reducing time by 15 minutes

					log.info("calender indian timezone::" + calendar);
					int HH = calendar.get(Calendar.HOUR_OF_DAY);
					int mm = calendar.get(Calendar.MINUTE);
					int ss = calendar.get(Calendar.SECOND);
					// log.info("reached here");
					String messageID = dbProcess.getmessageID(imo);
					log.info(messageID + "message id is");
					if (messageID != null) {
						dbProcess.setposReqTime(time, messageID);
					}
					log.info(HH + ":" + mm + ":" + ss);
					int frame = emailUtilities.calFrame(HH, mm, ss);
					log.info("creating set poll command");
					dbProcess.insertAspDcTxn(messageId, "", "ASP", "CSP", dbProcess.getCurrentTimestamp(),
							BigInteger.valueOf(4), dbProcess.getimo_num(mem, Integer.valueOf(dnidno)), "Set");

					String modelid = dbProcess.fetchmodelid(shipBorneEquipmentId);
					String modeltype = dbProcess.fetchmodeltype(modelid);
					commandType = pollCommandType.prepareSetPollCommand(dnidno, String.valueOf(mem),
							shipBorneEquipmentId, String.valueOf(frame), "4", modeltype);
					boolean flag7 = dbProcess.logCspTransaction(referenceCspId, "ASP", "CSP", dbProcess.getCurrentTimestamp(),
							pollCommandType.getPollCommand(), Integer.valueOf(dnidno), 0, mem, 4, "SET", "SET Command",
							false);
					/// checking if set is against Download DNID

					String setPollCommand = pollCommandType.getPollCommand();
					log.info(setPollCommand + "pollcommand is ");
					log.info("commandtype is " + commandType);

					// Create Start POll command

					commandType = pollCommandType.prepareStartPollCommand(dnidno, String.valueOf(mem),
							shipBorneEquipmentId, modeltype);

					String startPollCommand = pollCommandType.getPollCommand();
					log.info(startPollCommand + "pollcommand is ");
					log.info("commandtype is " + commandType);

					// insert SET poll command
					dbProcess.insertPollCommand(messageID, referenceCspId, startPollCommand, setPollCommand, imo,
							dnidno, mem, setSendingTime, startSendingTime, dbProcess.getCurrentTimestamp(), true, true);

					int rootCommandType = dbProcess.fetchCommandtype(referenceCspId);
					if (rootCommandType == 10) {
						String imo_no = dbProcess.getimo_num(mem, Integer.parseInt(dnidno));

						dbProcess.updateAspDnidDB(imo_no, "SET_FREQ_TRIGGERED", true);

					}
				} else {
					log.info("setting MAIL ");
					arg0.getOut().setHeader("MAIL", false);
					arg0.getOut().setBody(null);
				}

				break;

			// DELETE DNID
			case 11:
				log.info("command type is " + commandType);
				boolean flag8 = dbProcess.checkIfdeletePDNReceived(dnidno, referenceCspId, memNo);

				if ((flag8 == true) && (memNo != 0)) {
					log.info("First PDN for delete request");
					log.info("Generate Alert");
					dbProcess.generateAlert(shipBorneEquipmentId, 111);
					log.info("setting MAIL ");
					arg0.getOut().setHeader("MAIL", false);
					arg0.getOut().setBody(null);
				} else {
					log.info("setting MAIL ");
					arg0.getOut().setHeader("MAIL", false);
					arg0.getOut().setBody(null);
				}

				boolean flag9 = dbProcess.checkDnid(dnidno, "DNID Commands");

				if (flag9 == true) {
					log.info("member number= " + memNo);
					log.info("length= " + fourthLine.length);
					String imo_no;
					String status;
					imo_no = dbProcess.getimo_num(memNo, Integer.parseInt(dnidno));
					// method to check the status in portal_vessel_detail table.if it is
					// "DNID_DEL_REQ"
					status = dbProcess.fetchPortalStatus(imo_no);
					status = status.trim();
					log.info("status is " + status);

					if (status.equals("DNID_DEL_REQ")) {
						log.info("DNID DELETION");
						dbProcess.setstatusDnidDeleted(imo_no, "DNID_DELETED", dbProcess.getCurrentTimestamp());
						dbProcess.deleteDnidMemberNo(imo_no);
					} else if (status.equals("DNID_SEID_DEL_REQ")) {
						log.info("DNID SEID DELETION");
						dbProcess.setstatusDnidDeleted(imo_no, "SEID_DELETED", dbProcess.getCurrentTimestamp());
						dbProcess.deleteSEID_DNID(imo_no);
					}
				}
				break;

			default:
				break;
			}


			log.info("poll command is :::" + pollCommandType.getPollCommand());
			arg0.getOut().setHeader("choice", "PDN");
			log.info("[" + pollCommandType.getPollCommand() + "");

			if ((commandTypeinMail != 4) && (commandTypeinMail != 5)) {
				arg0.getOut().setHeader("from", dbProcess.getGolbalProperties("ASP"));
				arg0.getOut().setHeader("subject", pollCommandType.getPollCommand());
				arg0.getOut().setHeader("to", dbProcess.getGolbalProperties("CSP")); // lrit.testuser@gmail.com
				arg0.getOut().setHeader("request_status", "Success");
			}
		} catch (Exception e) {
			log.error(" in  processing _PDN " + e.getMessage() + " *** " + e.getCause() + " *** "
					+ e.getStackTrace().toString());
		}

	}

	public void processRequestForFrequency(int frame, int memNo, String dnidno, String shipBorneEquipmentId,
			String referenceCspId, int frequencyRate, String interval) {
		log.info("In processRequestForFrequency METHOD");
		Calendar calendar = Calendar.getInstance();
		String imo_no = dbProcess.getimo_num(memNo, Integer.parseInt(dnidno));
		int delay = dbProcess.getDelay(imo_no);
		Instant instant = emailUtilities.getSynctime(
				emailUtilities.timeStampToXMLGregorianCalender(dbProcess.getCurrentTimestamp()), frequencyRate, delay);
		Timestamp next = Timestamp.from(instant);
		log.info("next timestamp is" + next);
		calendar.setTimeInMillis(next.getTime());
		int HH = calendar.get(Calendar.HOUR_OF_DAY);
		int mm = calendar.get(Calendar.MINUTE);
		int ss = calendar.get(Calendar.SECOND);
		frame = emailUtilities.calFrame(HH, mm, ss);
		log.info("time is :: " + HH + ":" + mm + ":" + ss);
		String modelid = dbProcess.fetchmodelid(shipBorneEquipmentId);
		String modeltype = dbProcess.fetchmodeltype(modelid);
		String commandType = pollCommandType.prepareSetPollCommand(dnidno, String.valueOf(memNo), shipBorneEquipmentId,
				String.valueOf(frame), interval, modeltype);
		log.info("Command type is: " + commandType);
		boolean flag10 = dbProcess.logCspTransaction(referenceCspId, "ASP", "CSP", dbProcess.getCurrentTimestamp(),
				pollCommandType.getPollCommand(), Integer.valueOf(dnidno), 0, memNo, 4, "SET", "SET Command", false);
		log.info("Flag is " + flag10 + " Hence inserted in ASP-CSP Txn table");
		log.info("Exiting processRequestForFrequency METHOD");
	}

	public void processRequest(int frame, String shipBorneEquipmentId, String dnidno, int memNo, String referenceCspId,
			String interval) {
		log.info("In processRequest METHOD");
		String modelid = dbProcess.fetchmodelid(shipBorneEquipmentId);
		String modeltype = dbProcess.fetchmodeltype(modelid);
		String commandType = pollCommandType.prepareSetPollCommand(dnidno, String.valueOf(memNo), shipBorneEquipmentId,
				String.valueOf(frame), interval, modeltype);
		log.info("Command type is: " + commandType);
		boolean flag11 = dbProcess.logCspTransaction(referenceCspId, "ASP", "CSP", dbProcess.getCurrentTimestamp(),
				pollCommandType.getPollCommand(), Integer.valueOf(dnidno), 0, memNo, 4, "SET", "SET Command", false);
		log.info("Flag is " + flag11 + " Hence inserted in ASP-CSP Txn table");
		log.info("Exiting processRequest METHOD");
	}

	public int[]  calculateHHMMSS(Timestamp time, Calendar calendar) {
		int[] frame = {0,0,0};
		Calendar cal1 = Calendar.getInstance();
		cal1.setTimeInMillis(time.getTime());
		frame[0] = calendar.get(Calendar.HOUR_OF_DAY);
		frame[1] = calendar.get(Calendar.MINUTE);
		frame[2] = calendar.get(Calendar.SECOND);
		
		log.info("time is ::" + frame[0] + ":" + frame[1] + ":" + frame[2]);
		return frame;
	}
}
