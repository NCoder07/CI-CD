
  package in.gov.lrit.asp.dnid;
  
  import java.sql.Timestamp; import java.util.ArrayList; import
  java.util.Calendar;
  
  import in.gov.lrit.asp.common.DBUpdation; import
  in.gov.lrit.asp.common.PollCommands; import
  in.gov.lrit.asp.email.AspProcessEmail;
  
  import org.apache.camel.Exchange; import org.apache.camel.Processor; import
  org.imo.gisis.xml.lrit._2008.Response; import
  org.imo.gisis.xml.lrit.dnidrequest._2008.DNIDRequestType; import
  org.imo.gisis.xml.lrit.positionreport._2008.ShipPositionReportType; import
  org.imo.gisis.xml.lrit.positionrequest._2008.ShipPositionRequestType; import
  org.slf4j.Logger; import org.slf4j.LoggerFactory; import
  org.springframework.beans.factory.annotation.Autowired; import
  org.springframework.stereotype.Component;
  
  
	/*This class will be called by the Cron Timer function to check if we need to
	send the SET request or not. We should sent the set request 15 mins before
	the Position report Timeslot only,
	*/
  
  public class SetRequest implements Processor{
  
  public DBUpdation getDbProcess() {
	 return dbProcess;
  }

   public void setDbProcess(DBUpdation dbProcess) {
	 this.dbProcess = dbProcess;
   }
		
  @Autowired
  DBUpdation dbProcess;
  
  PollCommands pollCommandType = new PollCommands(); 
  Response resp = new Response();
  
  Logger log = (Logger) LoggerFactory.getLogger(SetRequest.class);
  
  
  public void process(Exchange arg0){
  log.info("inside timer component of SET");
  
  Timestamp currentTimestamp=dbProcess.getCurrentTimestamp();
  log.info("Current Timestamp is "+currentTimestamp);
  
  // Step 1: Log Position Rquest From DC log.info("[" +shipPositionRequestType.getMessageId() +"] Asp Position Request Received. ");
  
  //Retriving active SET requests from database
  ArrayList<Requestlist> list = dbProcess.FetchActiveRequests();
  
  log.info("************************* Total Active request found are: " + list.size() + "****************************");
  
  for (Requestlist requestlist : list) {
  
  log.info("[" +requestlist.message_id + "]" + "Processing the request...");
  
  
  Timestamp sendingTimestamp=requestlist.set_sending_timestamp;
  log.info("Sending Timestamp is "+sendingTimestamp );
  
  
  
  
  try {
  
  long milliseconds1 =currentTimestamp.getTime();
  //log.info("millisecond1"+milliseconds1);
  long milliseconds2 = sendingTimestamp.getTime(); 
  //log.info("millisecond2"+milliseconds2);
  long diff = milliseconds2 - milliseconds1; 
  //log.info("diff"+diff); 
  long diffMinutes = diff / (60 * 1000);
  
  long value = Math.abs(diffMinutes);
  log.info("Difference In Minutes: "+value); if (value < 15 ) {
  //dbProcess.generateAlert(shipEquipmentID, 118);
  
  
  log.info("[" +requestlist.message_id + "]" +
  "Request Processed Successfully"); Boolean updateStatus =
  dbProcess.updateSetPollDB(requestlist.getMessage_id()); if(updateStatus) {
  //String referenceCspId=null;
  dbProcess.logCspTransaction(requestlist.referencecspId, "ASP", "CSP",
  dbProcess.getCurrentTimestamp(), pollCommandType.getPollCommand(),
  Integer.valueOf(requestlist.dnid_no), 0, requestlist.member_no, 4, "SET",
  "SET Command", false);
  
  
  log.info("SET Request Inactived in Database...");
  arg0.getOut().setHeader("from", dbProcess.getGolbalProperties("ASP"));
  arg0.getOut().setHeader("subject",requestlist.set_pollcommand);
  arg0.getOut().setHeader("to", dbProcess.getGolbalProperties("CSP")); //
  arg0.getOut().setHeader("request_status", "Success");
  log.info("Mail Sent successfully!"); } } else { log.info("["
  +requestlist.message_id +
  "]"+"This is not the right timeslot. SET request is still Active."); } }
  catch (Exception e) { log.error("Error in sending SET poll command"); }
  
  
  
  }
  
  
  
  }
  
  }
  
 