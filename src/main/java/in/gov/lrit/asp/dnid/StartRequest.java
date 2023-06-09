
  package in.gov.lrit.asp.dnid;
  
  import java.sql.Timestamp;
import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.imo.gisis.xml.lrit._2008.Response;
import org.slf4j.Logger;
import
  org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import in.gov.lrit.asp.common.DBUpdation;
import
  in.gov.lrit.asp.common.PollCommands;
  
  
  public class StartRequest implements Processor {
  
  @Autowired
  DBUpdation dbProcess;
  
  public DBUpdation getDbProcess() {
		return dbProcess;
	}

	public void setDbProcess(DBUpdation dbProcess) {
		this.dbProcess = dbProcess;
	}
  
  PollCommands pollCommandType = new PollCommands(); 
  Response resp = new Response();
  
  Logger log = (Logger) LoggerFactory.getLogger(SetRequest.class);
  
  public void process(Exchange arg0){
  log.info("inside timer component of START");
  
  Timestamp startCurrentSendingTimestamp = dbProcess.getCurrentTimestamp();
  log.info("Start Timestamp is" + startCurrentSendingTimestamp);
  
  ArrayList<Requestlist> list= dbProcess.FetchStartActiveRequests();
  
  log.info("************************* Total Active request found are: " +list.size() + "****************************");
  
  
  for (Requestlist requestlist : list) {
  
  log.info("[" +requestlist.message_id + "]" + "Processing the request...");
  
  
  Timestamp startSendingTimestamp=requestlist.start_sending_timestamp;
  log.info(" Start Sending Timestamp is "+startSendingTimestamp );
  
  
  
  
  try {
  
  long milliseconds1 =startCurrentSendingTimestamp.getTime();
  //log.info("millisecond1"+milliseconds1); 
  long milliseconds2 = startSendingTimestamp.getTime(); 
  //log.info("millisecond2"+milliseconds2);
  long diff = milliseconds2 - milliseconds1; 
  //log.info("diff"+diff); 
  long diffMinutes = diff / (60 * 1000);
  
  long value = Math.abs(diffMinutes);
  log.info("Difference In Minutes: "+value); if (value < 22 ) {
  //dbProcess.generateAlert(shipEquipmentID, 118);
  
  
  log.info("[" +requestlist.message_id + "]" +
  "Request Processed Successfully"); Boolean updateStatus =
  dbProcess.updateStartPollDB(requestlist.getMessage_id()); if(updateStatus) {
  //String referenceCspId=null;
  dbProcess.logCspTransaction(requestlist.referencecspId, "ASP", "CSP",
  dbProcess.getCurrentTimestamp(), pollCommandType.getPollCommand(),
  Integer.valueOf(requestlist.dnid_no), 0, requestlist.member_no, 5, "START",
  "START Command", false);
  
  
  log.info("START Request Inactived in Database...");
  arg0.getOut().setHeader("from", dbProcess.getGolbalProperties("ASP"));
  arg0.getOut().setHeader("subject",requestlist.start_pollcommand);
  arg0.getOut().setHeader("to", dbProcess.getGolbalProperties("CSP")); 
  arg0.getOut().setHeader("request_status", "Success");
  log.info("Mail Sent successfully!"); } } else { log.info("["
  +requestlist.message_id +
  "]"+"This is not the right timeslot. START request is still Active."); } }
  catch (Exception e) { log.error("Error in sending START poll command"); }
  
  
  
  }
  
  
  
  } 
  }
 