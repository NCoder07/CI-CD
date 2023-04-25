package in.gov.lrit.asp.email;

import java.io.StringWriter;
import java.math.BigDecimal;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Response;

import org.imo.gisis.xml.lrit.positionreport._2008.ShipPositionReportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import in.gov.lrit.asp.exception.EmailFormatException;

@Component
public class EmailUtilities {

	Logger log = (Logger) LoggerFactory.getLogger(PdnProcess.class);
	
	public BigDecimal convertLocation(Double degree, Double minute, String direction) throws EmailFormatException {
		BigDecimal pos;
		if ((direction.equals("S") || direction.equals("W"))) {
			pos = new BigDecimal((degree + (minute / 60)));
			pos = pos.multiply(new BigDecimal(-1));

		} else if ((direction.equals("N") || direction.equals("E"))) {
			pos = new BigDecimal((degree + (minute / 60)));
		} else {
			throw new EmailFormatException("Location Invalid");
		}
		return pos;
	}

	boolean emailValidation(String from, String email) {

		if (from.endsWith(email)) {
			return true;
		} else
			return false;
	}

	String identifyEmailType(String body, String subject) {
		log.info("Inside Identify mail type method");
		String NDN = "Negative Delivery notification";
		String PDN = "Positive Delivery notification";
		String PR = "Maritime Mobile Position Report";
		String UR = "Unknown Type Position Report";
		String spam = "Spam";
		if (subject.contains(NDN)) {
			return NDN;
		} else if (subject.contains(PDN)) {
			return PDN;
		} else if (body.contains("Maritime Mobile Position Report")) {
			return PR;
		} else if (body.contains("Unknown Type Position Report")) {
			return UR;
		} else
			return spam;

	}

	public static String removeLastCharacter(String str) {
		String result = null;
		if ((str != null) && (str.length() > 0)) {
			result = str.substring(0, str.length() - 1);
		}
		return result;
	}

	String marshallPositionReport(ShipPositionReportType shipPositionReportType) throws JAXBException {
		JAXBContext jxbc = JAXBContext.newInstance(ShipPositionReportType.class);
		Marshaller marshl = jxbc.createMarshaller();
		marshl.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		StringWriter xmlBody = new StringWriter();
		org.imo.gisis.xml.lrit.positionreport._2008.ObjectFactory of = new org.imo.gisis.xml.lrit.positionreport._2008.ObjectFactory();
		marshl.marshal(of.createShipPositionReport(shipPositionReportType), xmlBody);
		return xmlBody.toString();
	}

	public String extractResponse(Response<?> response) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Response.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			StringWriter sw = new StringWriter();
			jaxbMarshaller.marshal(response, sw);
			return sw.toString();
		} catch (Exception ex) {

			log.error("issue in extractResponse marshalling returning null");
		}
		return null;
	}

	public String marshellResponseBody(Response<?> resp) throws JAXBException {
		StringWriter xmlBody = new StringWriter();
		JAXBContext jxbc = JAXBContext.newInstance(Response.class);
		Marshaller marshl = jxbc.createMarshaller();
		marshl.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		org.imo.gisis.xml.lrit.positionreport._2008.ObjectFactory of = new org.imo.gisis.xml.lrit.positionreport._2008.ObjectFactory();
		marshl.marshal(resp, xmlBody);
		return xmlBody.toString();
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
			e.printStackTrace();
			return null;
		}
	}

	public Instant getSynctime(XMLGregorianCalendar start_time, int frequencyRate, int delay) {

		log.info("Inside Synchronise Method");
		int Basetime = 1440; // 24hrs   (base time is in minutes)
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
				log.debug("Modified BaseTime :  " + Basetime);
				A = Basetime - frequencyRate;
				log.debug("Modified A : " + A);
				B = A - initialReqTimeMinutes;
				log.debug("Modified B : " + B);
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
		log.info("Date : " + d.toString());
		log.info("Instant :" + instant);
		return instant;

	}

	public int calFrame(int HH, int mm, int ss) {
		int frameNumber = (int) (((HH * 3600) + (mm * 60) + ss) / (8.64));
		log.info("Frame number::" + frameNumber);
		return frameNumber;
	}
}
