package in.gov.lrit.asp.lritAPI;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.imo.gisis.xml.lrit.positionreport._2008.ShipPositionReportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

//import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.lrit.asp.common.DBUpdation;
import in.gov.lrit.asp.email.AspProcessEmail;

public class ApiRequestProcessor implements Processor{

	
	DBUpdation dbProcess;
	public DBUpdation getDbProcess() {
		return dbProcess;
	}

	public void setDbProcess(DBUpdation dbProcess) {
		this.dbProcess = dbProcess;
	}
	@Override
	public void process(Exchange exchange) throws Exception {

		Logger log = (Logger) LoggerFactory.getLogger(AspProcessEmail.class);

		String flag = (String) exchange.getIn().getHeader("processflag");

		if(flag.equals("1")){
			ShipPositionReportType shipPosition = (ShipPositionReportType) exchange.getIn().getBody();
			
			String countryName = dbProcess.getCountryName(shipPosition.getDataUserProvider());
			
			String shipCode = shipPosition.getShipType().trim(), shipType = "NA";
			if(shipCode.equalsIgnoreCase("0100"))
				shipType = "Passenger";
			else if(shipCode.equalsIgnoreCase("0200"))
				shipType = "Cargo";
			else if(shipCode.equalsIgnoreCase("0300"))
				shipType = "Tanker";
			else if(shipCode.equalsIgnoreCase("0400"))
				shipType = "Mobileossshore";
			else if(shipCode.equalsIgnoreCase("9900"))
				shipType = "Other_ships";

			ShipDetail requestObj = new ShipDetail();
			requestObj.setMmsi(shipPosition.getMMSINum());
			requestObj.setImo(shipPosition.getIMONum());
			requestObj.setLatitude(shipPosition.getLatitude());
			requestObj.setLongitude(shipPosition.getLongitude());
			requestObj.setVesselName(shipPosition.getShipName());
			requestObj.setVesselType(shipType);
			requestObj.setVesselTypeCode(shipCode);
			requestObj.setVesselTypeCargo("NA");
			requestObj.setVesselClass("NA");
			requestObj.setCountry(countryName);
			requestObj.setDestination("NA");
			requestObj.setStatus("NA");
			requestObj.setHeading("NA");

			LRITAPIDashBoard lad = new LRITAPIDashBoard();
			List<ShipDetail> shipDetailList = new ArrayList<ShipDetail>();
			shipDetailList.add(requestObj);
			lad.setShipDetails(shipDetailList);

			// Creating Object of ObjectMapper define in Jakson Api
			ObjectMapper mapper = new ObjectMapper();
			String jsonString = null;
			try {
				jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(lad);
				//System.out.println("Request string:" + jsonString);

			} catch (Exception e) {
				e.printStackTrace();
			}
			String rowno = dbProcess.insertApiDashboard(jsonString);

			try {
				exchange.getOut().setHeader("rowno", rowno);
				exchange.getOut().setBody(jsonString);
				//System.out.println("Request Send to api:" + jsonString);
				log.info("Ship Position Send: " + jsonString);
			}catch(Exception e){
				e.printStackTrace();
			}
		}else{
			log.info("Processing response");
			try{
				String response = exchange.getIn().getBody(String.class);
				String rowno = (String) exchange.getIn().getHeader("rowno");
				log.info("Response: " + response + "");
				dbProcess.updateApiDashboard(rowno, response);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
}
