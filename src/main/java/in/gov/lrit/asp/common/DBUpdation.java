package in.gov.lrit.asp.common;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import in.gov.lrit.asp.dnid.Requestlist;
import in.gov.lrit.asp.email.AspProcessEmail;
import lombok.Data;

/**
 * <h1>DBUpdation.java</h1> This class handles all the database changes .
 * 
 * @copyright 2019 CDAC Mumbai. All rights reserved
 * @author lrit-team
 * @version 1.0
 */

@Data
public class DBUpdation {

	final static String GET_CGOWNER_FROM_IMO_NO = "select requestors_lrit_id from vw_vessel_imo_lritid where imo_no=?";

	final static String GetCurrentDdpVersion = "SELECT ddpversion from public.ddp_version; ";
	final static String GetImoNum = "SELECT shipborne_equipment_id, imo_no FROM public.portal_ship_equipement where member_no=? and dnid_no=?;";

	final static String FetchDetailQuery = "SELECT  mmsi_no, vessel_name, imo_vessel_type FROM public.portal_vessel_details WHERE imo_no = ?;";
	final static String FetchSEID = "select shipborne_equipment_id from portal_ship_equipement where imo_no = ?";
	final static String FetchCGID = "select cg_lrit_id from asp_terminal_frequency where imo_no = ?";
	final static String fetchFrequency = "select min(frequency_rate) from asp_terminal_frequency where imo_no=?";
	final static String fetchCGIDAgainstFrequency = "Select cg_lrit_id from asp_terminal_frequency where frequency_rate=? and imo_no=?";

	final static String GET_CSP_TXN_ID = "select csp_txn_id from public.asp_csp_txn where dnid_no=? and member_no=? order by timestamp desc limit 1";

	final static String FetchSetRequests = "SELECT * FROM public.asp_poll_request WHERE set_is_active = true";
	final static String FetchStartRequest = "SELECT * FROM public.asp_poll_request WHERE start_is_active = true";

	DataSource dataSource;
	DataSource ddpds;

	String shipBorneEquipmentId;
	String dnidNo;

	/*
	 * DBUpdation(DataSource dataSource) { this.dataSource = dataSource; }
	 */

	public String getShipBorneEquipmentId() {
		return shipBorneEquipmentId;
	}

	public void setShipBorneEquipmentId(String shipBorneEquipmentId) {
		this.shipBorneEquipmentId = shipBorneEquipmentId;
	}

	public String getDnidNo() {
		return dnidNo;
	}

	public void setDnidNo(String dnidNo) {
		this.dnidNo = dnidNo;
	}

	public void setDdpds(DataSource ddpds) {
		this.ddpds = ddpds;
	}

	public DataSource getDdpds() {
		return ddpds;
	}

	public DataSource getDataSource() {
		log.info("inside getDataSource: " + this.dataSource);
		return this.dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	Logger log = (Logger) LoggerFactory.getLogger(AspProcessEmail.class);

	/**
	 * 
	 * This method handles insertion asp_dnid_txn
	 * 
	 * @param This method takes messageId,Dnid number, Member Number ,timestamp and
	 *             Ocean region as input.
	 * @return boolean
	 */

	public boolean insertAspDninDB(String message_id, String dnid_no, int member_no, Timestamp currentTimestamp,
			BigInteger ocean_region, String imo) {

		final String InsertTransaction = "INSERT INTO public.asp_dnid_txn(\n"
				+ "	message_id, dnid_no, member_no, timestamp, ocean_region,imo_no,is_active)\n"
				+ "	VALUES (?, ?, ?, ?, ?, ?, ?)";

		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(InsertTransaction)) {
			stmt.setString(1, message_id);
			stmt.setInt(2, Integer.parseInt(dnid_no));
			stmt.setInt(3, (member_no));
			stmt.setObject(4, currentTimestamp);
			stmt.setInt(5, ocean_region.intValue());
			stmt.setString(6, imo);
			stmt.setBoolean(7, true);
			stmt.executeUpdate();
			log.info("[ " + message_id + "] Inserted in ASP Dnid   ");

			stmt.close();
			con.close();
			return true;
		} catch (Exception e) {
			log.error("Error in inserting asp_dnid_txn " + e.getMessage() + " " + e.getStackTrace());
			return false;
		}

	}

	/**
	 * 
	 * This method handles updates asp_dnid_txn
	 * 
	 * @param This method takes messageId and request status as input.
	 * @return boolean
	 */

	public boolean updateAspDnidDB(String imo_no, String request_status, boolean Is_active) {

		final String updateTransaction = "UPDATE public.asp_dnid_txn SET request_status = ? where imo_no=? and is_active =?";

		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(updateTransaction)) {
			stmt.setString(1, request_status);
			stmt.setString(2, imo_no);
			stmt.setBoolean(3, true);
			stmt.executeUpdate();
			stmt.close();
			con.close();
			return true;
		} catch (Exception e) {
			log.error("Error in updating asp_dnid_db " + e.getMessage() + " " + e.getStackTrace());
			return false;
		}
	}

	/**
	 * 
	 * This method fetches ship info from portal_ship_quipment
	 * 
	 * @param This method takes imo_no as input.
	 * @return string[]
	 */

	public String[] getShipInfo(String imo_no) {

		String[] data = new String[3];
		final String selectTransaction = "Select member_no,shipborne_equipment_id,dnid_no From public.portal_ship_equipement \n"
				+ "\n where imo_no= ?";

		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(selectTransaction)) {
			stmt.setString(1, imo_no);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				data[0] = rs.getString(1);
				data[1] = rs.getString(2);
				data[2] = rs.getString(3);
			}

			rs.close();
			stmt.close();
			con.close();
		} catch (Exception e) {
			log.error("fetching seid memno. dnidno " + e.getMessage() + e.getStackTrace());
		}
		return data;
	}

	/**
	 * 
	 * This method handles insertion asp_dc_txn
	 * 
	 * @param This method takes
	 *             messageId,reference_message_id,received_from,destination_id,
	 *             timestamp as input.
	 * @return boolean
	 */

	public boolean insertAspDcTxn(String message_id, String reference_messageid, String received_from,
			String destination_id, Timestamp currentTimestamp, BigInteger message_type, String imo_no,
			String message_status) {

		final String InsertTransaction = "INSERT INTO public.asp_dc_txn(message_id, reference_messageid, received_from, destination_id,timestamp, message_type, imo_no, message_status)VALUES (?, ?, ?, ?, ?, ?, ?, ?);";

		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(InsertTransaction)) {

			stmt.setString(1, message_id);
			stmt.setString(2, reference_messageid);
			stmt.setString(3, received_from);
			stmt.setString(4, destination_id);
			stmt.setTimestamp(5, currentTimestamp);
			stmt.setInt(6, message_type.intValue());
			stmt.setString(7, imo_no);
			stmt.setString(8, message_status);

			stmt.executeUpdate();
			log.info("[ " + message_id + "] Inserted in ASP DC Transaction  ");
			stmt.close();
			con.close();
			return true;
		} catch (Exception e) {
			log.error("Error in insertinf in asp_csp_txn " + e.getMessage() + " " + e.getStackTrace());
			return false;
		}
	}

	/**
	 * 
	 * This method checks asp_terminal_frequency
	 * 
	 * @param This method takes dataUserRequestor,imo_no as input.
	 * @return boolean
	 */

	public boolean checkterminalfrequency(String dataUserRequestor, String imo_no) {

		log.info("checking terminal frequency table for IMO No.::" + "[" + imo_no + "]");
		final String selectTransaction = "Select * From public.asp_terminal_frequency where cg_lrit_id=? and imo_no=?;";
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(selectTransaction)) {
			stmt.setString(1, dataUserRequestor);
			stmt.setInt(2, Integer.parseInt(imo_no.trim()));
			ResultSet rs = stmt.executeQuery();
			if (rs.next() == false) {
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			log.error("Error in checking terminal frequency" + e.getMessage() + " " + e.getStackTrace());
			return false;
		}
	}

	/**
	 * 
	 * This method handles insertion asp_position_request
	 * 
	 * @param This method takes messageId,imo number, request type, timestamp and
	 *             shipborneEquipment ID as input.
	 * @return boolean
	 */

	public boolean insertPosRequest(String message_id, BigInteger messageType, String imoNO,
			String shipBorneEquipmentId, BigInteger accessType, BigInteger requestType, XMLGregorianCalendar timeStamp,
			XMLGregorianCalendar startTime, XMLGregorianCalendar stopTime, String status) {
		log.info("Inserting in Position Request Table" + "[" + message_id + "]");
		final String insertPositionRequest = "INSERT INTO public.asp_position_request(message_id, message_type, imo_no, shipborne_equipment_id, access_type, request_type, transmit_timestamp, req_start_time, req_stop_time,status)VALUES (?, ?, ?, ?, ?,  ?, ?, ?, ?,  ?);";
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(insertPositionRequest)) {
			stmt.setString(1, message_id);
			stmt.setInt(2, messageType.intValue());
			stmt.setString(3, imoNO);
			stmt.setString(4, shipBorneEquipmentId);
			stmt.setInt(5, accessType.intValue());
			stmt.setInt(6, requestType.intValue());
			Timestamp timeStamp1 = new Timestamp(timeStamp.toGregorianCalendar().getTimeInMillis());
			stmt.setTimestamp(7, timeStamp1);
			Timestamp start_time = new Timestamp(startTime.toGregorianCalendar().getTimeInMillis());
			Timestamp stop_time = new Timestamp(stopTime.toGregorianCalendar().getTimeInMillis());
			stmt.setTimestamp(8, start_time);
			stmt.setTimestamp(9, stop_time);
			stmt.setString(10, status);
			stmt.executeUpdate();
			log.info("Inserted in Position Request Table" + "[" + message_id + "]");
			stmt.close();
			con.close();
			return true;
		} catch (Exception e) {
			log.error("Error in inserting position request" + e.getMessage() + " " + e.getStackTrace());
			return false;
		}
	}

	/**
	 * 
	 * This method checks in asp_position_request
	 * 
	 * @param This method takes Imo_no and seid as input.
	 * @return boolean
	 */

	public boolean checkPositionRequest(String imoNo, String shipBorneEquipmentId2) {

		log.info("checking in Position Request table for" + "[" + imoNo + "]" + "[" + shipBorneEquipmentId2 + "]");
		final String selectTransaction = "Select * From public.asp_position_request \n"
				+ "\n where imo_no=? and shipborne_equipment_id = ?;";
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(selectTransaction)) {
			stmt.setString(1, imoNo);
			stmt.setString(2, shipBorneEquipmentId2);

			ResultSet rs = stmt.executeQuery();
			if (rs.next() == false) {
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			log.error("Error in method checkPositionRequest " + e.getMessage() + " " + e.getStackTrace());
			return false;
		}
	}

	/**
	 * 
	 * This method sets status as Inactive in asp_position_request
	 * 
	 * @param This method takes Imo_no and seid as input.
	 * @return boolean
	 */

	public void setStatusInactive(String imoNo, String shipBorneEquipmentId2, String status) {

		log.info("Setting status Inactive for " + "[" + imoNo + "]" + "[" + shipBorneEquipmentId2 + "]");
		final String setInactivequery = "UPDATE public.asp_position_request SET status=? WHERE shipborne_equipment_id=? and imo_no=? ;";
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(setInactivequery)) {
			stmt.setString(1, status);
			stmt.setString(2, shipBorneEquipmentId2);
			stmt.setString(3, imoNo);
			stmt.executeUpdate();
			stmt.close();
			con.close();
		} catch (Exception e) {
			log.error("Error in changing  status" + e.getMessage() + " " + e.getStackTrace());
		}
	}

	/**
	 * 
	 * This method sets terminates request asp_terminal_frequency
	 * 
	 * @param This method takes datarequestor and imo_no as input.
	 * @return boolean
	 */

	public void terminaterequest(String dataUserRequestor, String imo_no) {

		log.info("Terminating the Existing Request");
		final String terminateRequestquery = "DELETE FROM public.asp_terminal_frequency  WHERE imo_no=? and cg_lrit_id=? ;";
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(terminateRequestquery)) {
			stmt.setInt(1, Integer.valueOf(imo_no));
			stmt.setString(2, dataUserRequestor);
			stmt.executeUpdate();
			stmt.close();
			con.close();
		} catch (Exception e) {
			log.error("Error in terminating request" + e.getMessage() + " " + e.getStackTrace());
		}
	}

	/**
	 * 
	 * This method handles insertion asp_terminal_frequency
	 * 
	 * @param This method takes
	 *             message_id,imo_no,dnid_no,member_no,cg_lrit_id,frequency as
	 *             input.
	 * @return void
	 */

	public void insertTerminalfrequency(String message_id, String imo_no, String dnidNo2, int member_no,
			String dataUserRequestor, int frequencyRate, XMLGregorianCalendar start_time, String status) {

		final String insertTerminalFrequency = "INSERT INTO public.asp_terminal_frequency(message_id, imo_no, dnid_no, member_no, cg_lrit_id, frequency_rate, starttime, status)VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(insertTerminalFrequency)) {
			stmt.setString(1, message_id);
			stmt.setInt(2, Integer.parseInt(imo_no));
			stmt.setInt(3, Integer.parseInt(dnidNo2));
			stmt.setInt(4, member_no);
			stmt.setString(5, dataUserRequestor);
			stmt.setInt(6, frequencyRate);
			Calendar c = start_time.toGregorianCalendar();
			Timestamp start_time1 = new Timestamp(c.getTimeInMillis());
			stmt.setObject(7, start_time1);
			stmt.setString(8, status);
			stmt.executeUpdate();
			stmt.close();
			con.close();
			log.info("[ " + message_id + "] Inserted in Terminal Frequency Table  ");
		} catch (Exception e) {
			log.error("Error in Inserting Terminal Frequesncy::::" + e.getMessage() + e.getStackTrace());
		}
	}

	/**
	 * 
	 * This method handles insertion in asp_position_request
	 * 
	 * @param This method takes reference_cspid,dnid,member_no,subject,timestamp and
	 *             seid as input.
	 * @return boolean
	 */

	public boolean logCspTransaction(String reference_cspid, String received_from, String destination_id,
			Timestamp timestamp, String mail_subject, int dnid_no, int seid, int member_no, Integer command_type,
			String message_body, String message_status, boolean is_set) {
		log.info("Insering in csp Transaction table");
		Connection conn = null;
		PreparedStatement psmt = null;

		String logCspTransactionQuery = "INSERT INTO public.asp_csp_txn( reference_cspid, received_from, destination_id, timestamp,mail_subject, dnid_no, seid, member_no, command_type, message_body, message_status, is_set) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(logCspTransactionQuery);
			psmt.setString(1, reference_cspid);
			psmt.setString(2, received_from);
			psmt.setString(3, destination_id);
			psmt.setObject(4, timestamp);
			psmt.setString(5, mail_subject);
			psmt.setInt(6, dnid_no);
			psmt.setInt(7, seid);
			psmt.setInt(8, member_no);
			psmt.setObject(9, command_type, java.sql.Types.INTEGER);
			psmt.setString(10, message_body);
			psmt.setString(11, message_status);
			psmt.setBoolean(12, is_set);
			log.info("query is " + psmt.toString());
			psmt.executeUpdate();
			log.info(" Inserted in ASP-CSP Transaction  table ");
			psmt.close();
			conn.close();
			return true;
		} catch (SQLException e) {
			log.error("Inserting in transaction Table " + e.getMessage() + e.getStackTrace());
			return false;
		} finally {
			try {
				psmt.close();

				conn.close();
			} catch (SQLException e) {
				log.error("Inserting in transaction Table  " + e.getMessage() + " " + e.getStackTrace());
			}
		}
	}

	/**
	 * 
	 * This method handles insertion in asp_shipposition
	 * 
	 * @param This method takes
	 *             message_id,lattitude,longitude,timestamp1.timestamp2,
	 *             timestamp3,imo,speed,dc_id and sshipname as input.
	 * @return boolean
	 */
	public boolean logshippositionReport(String message_id, String latitude, String longitude, Timestamp timestamp1,
			String asp_id, String csp_id, String shipborne_equipment_id, Timestamp timestamp2, Timestamp timestamp3,
			String dc_id, BigDecimal speed, BigDecimal course, int imo_no, String ocean_region, String mmsi_no,
			BigInteger message_type, String data_user_requestor, String data_user_provider, String ddpversion_no,
			String shiptype, String shipname, Timestamp timestamp4, int responsetype) {
		log.info("Inside log logshippositionReport");
		Connection conn = null;
		PreparedStatement psmt = null;
		String positionReportQuery = "INSERT INTO public.asp_shipposition(message_id, latitude, longitude, timestamp1, asp_id, csp_id,shipborne_equipment_id, timestamp2, timestamp3, dc_id, speed,course, ocean_region, imo_no, mmsi_no, message_type, data_user_requestor,data_user_provider, ddpversion_no, ship_type, ship_name, timestamp4,response_type)VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?,?);";

		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(positionReportQuery);
			psmt.setString(1, message_id);
			psmt.setDouble(2, convertDDMtoDD(latitude));
			psmt.setDouble(3, convertDDMtoDD(longitude));
			/*
			 * psmt.setDouble(2, latitude); psmt.setDouble(3, longitude);
			 */
			psmt.setObject(4, timestamp1);
			psmt.setString(5, asp_id);
			psmt.setString(6, csp_id);
			psmt.setString(7, shipborne_equipment_id);
			psmt.setObject(8, timestamp2);
			psmt.setObject(9, timestamp3);
			psmt.setString(10, dc_id);
			psmt.setBigDecimal(11, speed);
			psmt.setBigDecimal(12, course);
			psmt.setString(13, ocean_region);
			psmt.setInt(14, imo_no);
			psmt.setString(15, mmsi_no);
			psmt.setBigDecimal(16, new BigDecimal(message_type));
			psmt.setString(17, data_user_requestor);
			psmt.setString(18, data_user_provider);
			psmt.setString(19, ddpversion_no);
			psmt.setString(20, shiptype);
			psmt.setString(21, shipname);
			psmt.setObject(22, timestamp4);
			psmt.setInt(23, responsetype);
			log.info("Query : " + psmt.toString());
			psmt.executeUpdate();
			psmt.close();
			conn.close();
			log.info("[ " + message_id + "] Inserted in Ship Position Report table  ");
			log.info("Latitude And Longitude inserted in DB : Lat :[ " + latitude + " ] Long : [ " + longitude + " ]");
			return true;
		} catch (SQLException e) {
			log.error("Error in logshippositionReport: " + e.getLocalizedMessage());
			return false;
		} finally {
			try {
				psmt.close();

				conn.close();
			} catch (SQLException e) {
				log.error("Error in logshippositionReport:  " + e.getLocalizedMessage());
			}
		}
	}

	/**
	 * 
	 * This method converts DDM to DD
	 * 
	 * @param This method takes ddm as input.
	 * @return double
	 */

	public double convertDDMtoDD(String ddm) {

		// example : 42.26.04.N , then degree = 42, decimal minutes = 26.04,
		// direction = N
		// example : 152.05.36.W , then degree = 152, decimal minutes = 05.36,
		// direction = W
		// degree range : (0 to 89, 0 to 179) - (lat,long)
		// minutes range - upto 2 decimal places : (0 to 59.99)
		try {
			log.info("ddm: " + ddm);
			String[] output = ddm.split("\\.");

			DecimalFormat df = new DecimalFormat("00.000000");
			df.setRoundingMode(RoundingMode.HALF_UP);
			int degree = 0;
			double minutes = 0;
			String direction = null;
			log.debug("decimal parts : " + output.length);
			if (output.length == 4) {
				degree = Integer.parseInt(output[0]);
				minutes = Double.parseDouble(output[1] + "." + output[2]);
				direction = output[3];
			} else if (output.length == 3) {
				degree = Integer.parseInt(output[0]);
				minutes = Double.parseDouble(output[1]);
				direction = output[2];
			}

			log.debug("Degree : " + degree);
			log.debug("Minutes : " + minutes);
			log.debug("Direction : " + direction);

			BigDecimal pos = new BigDecimal((degree + (minutes / 60)));
			if ((direction.equals("S") || direction.equals("W"))) {
				pos = pos.multiply(new BigDecimal(-1));
			}
			log.info("Position : " + pos);
			log.info("Formatted Position : " + df.format(pos));
			log.info("Float Format Position : " + Double.parseDouble(df.format(pos)));
			return Double.parseDouble(df.format(pos));
		} catch (Exception e) {
			log.error("error in conversion : " + e.getMessage());
			return 0;
		}

	}

	/**
	 * 
	 * This method fetches lrit_id from lrit_component
	 * 
	 * @param This method takes name as input.
	 * @return string
	 */

	public String getId(String name) {

		final String getDcIdquery = "SELECT lrit_id FROM public.lrit_component_mst WHERE component_name=?;";
		String id = null;
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(getDcIdquery)) {
			stmt.setString(1, name);
			ResultSet rs = stmt.executeQuery();

			rs.next();
			id = rs.getString(1);
			rs.close();
			stmt.close();
			con.close();
			log.info("[ " + id + "] REquired ID got from Componenet table  ");
		} catch (SQLException e) {
			log.error("Error in fetching cgid " + e.getMessage() + "  " + e.getStackTrace());
		}
		return id;
	}

	/**
	 * 
	 * This method fetches ship details from portal_vessel_detail
	 * 
	 * @param This method takes imo as input.
	 * @return string[]
	 */

	public String[] fetchShipDetails(String imoNumber) {

		Connection conn = null;
		PreparedStatement psmt = null;
		String[] data = new String[3];
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(FetchDetailQuery);
			psmt.setString(1, imoNumber);
			ResultSet rs = psmt.executeQuery();
			while (rs.next()) {
				data[0] = rs.getString(1);
				data[1] = rs.getString(2);
				data[2] = rs.getString(3);
			}

			// ---------------------Start-----------------------
			rs.close();
		} catch (SQLException e) {
			log.error("Error in fetching ship details " + e.getMessage() + " " + e.getStackTrace());
		}

		finally {
			try {

				psmt.close();
				conn.close();

			} catch (SQLException e) {

				log.error("Error in fetching ship details " + e.getMessage() + " " + e.getStackTrace());
			}
		}
		// ------------------------End-----------------------

		/*
		 * rs.close(); psmt.close(); conn.close(); } catch (SQLException e) {
		 * log.error("Error in fetching ship details " + e.getMessage() + " " +
		 * e.getStackTrace()); }
		 */
		return data;
	}

	/**
	 * 
	 * This method fetches imo and seid from portal_shipeuipment table
	 * 
	 * @param This method takes dnid and member_no as input.
	 * @return string[]
	 */

	public String[] getImoNumberandshipid(String dnid, String mem) {
		Connection conn = null;
		PreparedStatement psmt = null;
		String[] data = new String[2];
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(GetImoNum);
			psmt.setInt(1, Integer.parseInt(mem));
			psmt.setInt(2, Integer.parseInt(dnid));
			try (ResultSet rs = psmt.executeQuery();) {
				while (rs.next()) {
					data[0] = rs.getString(1);
					data[1] = rs.getString(2);
				}
			}
		} catch (SQLException e) {
			log.error("Error in fetching ship details " + e.getMessage() + " " + e.getStackTrace());
		}

		finally {
			try {
				psmt.close();
				conn.close();

			} catch (SQLException e) {

				log.error("Error in fetching ship details " + e.getMessage() + " " + e.getStackTrace());
			}
		}
		return data;
	}

	/**
	 * 
	 * This method updates asp_csp_transaction
	 * 
	 * @param This method takes csp_id and status as input.
	 * 
	 * @return boolean
	 */

	public boolean updateCSPTransaction(String cspid, String status, Integer commandtype) {
		Connection conn = null;
		PreparedStatement psmt = null;
		String updateCSPTransactionquery = "UPDATE public.asp_csp_txn SET  message_status=? WHERE csp_txn_id=? and command_type=?;";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(updateCSPTransactionquery);
			psmt.setString(1, status);
			psmt.setString(2, cspid);
			psmt.setInt(3, commandtype);
			psmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Error in updating asp_csp_txn " + e.getMessage() + " " + e.getStackTrace());
			return false;
		} finally {
			try {
				psmt.close();

				conn.close();
			} catch (SQLException e) {
				log.error("Error in updating asp_csp_txn " + e.getMessage() + " " + e.getStackTrace());
			}

		}
		return true;

	}

	/**
	 * 
	 * This method checks if the transaction was initiated from asp
	 * 
	 * @param This method takes dnid and referenceid as input.
	 * @return boolean
	 */

	public boolean checkInitiationFromASP(String dnidno, String referenceid) {
		log.info("checking if initiated from ASP");
		Connection conn = null;
		PreparedStatement psmt = null;
		boolean flag = false;
		String checkfromASPquery = "SELECT count(*) FROM public.asp_csp_txn where  received_from=? and dnid_no=? and reference_cspid=?";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(checkfromASPquery);

			psmt.setString(1, "ASP");
			psmt.setInt(2, Integer.parseInt(dnidno));
			psmt.setString(3, referenceid);
			try (ResultSet rs = psmt.executeQuery();) {
				rs.next();
				int n = rs.getInt(1);
				if (n != 0) {
					// log.info("result set has got something");
					log.info("n =" + n);
					flag = true;
				} else
					flag = false;
			}
		} catch (SQLException e) {
			log.error("Error inchecking if request initiated from asp " + e.getMessage() + " " + e.getStackTrace());
			return flag;
		} finally {
			try {
				psmt.close();

				conn.close();
				return flag;
			} catch (SQLException e) {
				log.error("Error in updating asp_csp_txn " + e.getMessage() + " " + e.getStackTrace());
			}

		}
		return true;
	}

	/**
	 * 
	 * This method checks is_set parameter in asp_csp_txn
	 * 
	 * @param This method takes dnid as input.
	 * @return boolean
	 */

	public boolean checkIsSet(String dnidno) {
		Connection conn = null;
		PreparedStatement psmt = null;
		boolean flag = false;
		String checkIsSetquery = "SELECT count(*) FROM public.asp_csp_txn where  is_set=? and dnid_no=?;";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(checkIsSetquery);
			psmt.setBoolean(1, true);
			psmt.setInt(2, Integer.parseInt(dnidno));
			try (ResultSet rs = psmt.executeQuery();) {
				if (rs != null) {
					flag = true;
				} else
					flag = false;
			}
		} catch (SQLException e) {
			log.error("Error in is_set status  " + e.getMessage() + " " + e.getStackTrace());
			return flag;
		} finally {
			try {
				psmt.close();

				conn.close();
				return flag;
			} catch (SQLException e) {
				log.error("Error in is_set status  " + e.getMessage() + " " + e.getStackTrace());
			}

		}
		return true;
	}

	/**
	 * 
	 * This method fetches ddp_version from ddp_version view
	 * 
	 * @param This method takes no input
	 * 
	 * @return string
	 */

	public String getCurrentDDpVersion() throws SQLException {
		try (Connection con = ddpds.getConnection();
				PreparedStatement ps = con.prepareStatement(GetCurrentDdpVersion)) {
			ResultSet rs = ps.executeQuery();
			rs.next();
			return rs.getString(1);
		}
	}

	/**
	 * 
	 * This method generates Message_id
	 * 
	 * @param This method takes cglrit as input.
	 * @return string
	 */

	public String generateMessageID(String cglrit) {
		LocalDateTime ldt = LocalDateTime.now();
		int date = ldt.getDayOfMonth();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		String instance = dtf.format(ldt);
		// int random = (int)(Math.random() * 100000);
		String random = "";
		try (Connection con = this.dataSource.getConnection();
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select nextval('lrit_messageid_seq')");) {
			rs.next();
			random += "" + rs.getInt(1);
		} catch (SQLException e) {
			log.error("ERROR" + e.getMessage());
		}
		String id = cglrit + instance + random;
		return id;
	}

	/**
	 * 
	 * This method gets current instance
	 * 
	 * @param This method takes no input.
	 * @return XMLGregorianCalendar
	 */

	public XMLGregorianCalendar getCurrentInstance() throws DatatypeConfigurationException {
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
		XMLGregorianCalendar now = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
		return now;
	}

	public Timestamp getCurrentTimestamp() {
		Calendar c = Calendar.getInstance();
		return new Timestamp(c.getTimeInMillis());
	}

	/**
	 * 
	 * This method inserts into cspdcmapping
	 * 
	 * @param This method takes messageid and cspid input.
	 * @return void
	 */

	public void insertcspdcmapping(String messageid, String cspid) {

		Connection conn = null;
		PreparedStatement psmt = null;
		String positionReportQuery = "INSERT INTO public.asp_dc_csp_mapping_txn(message_id, csp_id) VALUES (?, ?);";

		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(positionReportQuery);
			psmt.setString(1, messageid);
			psmt.setString(2, cspid);
			psmt.executeUpdate();
		}

		catch (SQLException e) {
			log.error("Error in inserting in asp_dc_mapping  " + e.getMessage() + " " + e.getStackTrace());

		} finally {
			try {
				psmt.close();

				conn.close();
			} catch (SQLException e) {
				log.error("Error in inserting in asp_dc_mapping  " + e.getMessage() + " " + e.getStackTrace());
			}

		}
		// log.info("[ "+messageid+" "+cspid+"] Inserted in ASP CSP DC mapping
		// table ");

	}

	public int gen() {
		Random r = new Random(System.currentTimeMillis());
		return 10000 + r.nextInt(20000);
	}

	public boolean checkDnid(String dnid, String status) {
		log.info("Checking DNID no. in ASP CSP Transaction");
		Connection conn = null;
		PreparedStatement psmt = null;
		boolean flag = false;
		String checkDnid = "SELECT count(*) FROM public.asp_csp_txn where  message_status=? and dnid_no=?;";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(checkDnid);
			psmt.setString(1, status);
			psmt.setInt(2, Integer.parseInt(dnid));
			try (ResultSet rs = psmt.executeQuery();) {
				rs.next();
				int n = rs.getInt(1);
				if (n != 0) {
					// log.info("result set has got something");

					log.info("n is " + n);
					log.info("result set not empty");
					flag = true;
				} else
					flag = false;
			}
		} catch (SQLException e) {

			log.error("Error in check dnid method  " + e.getMessage() + " " + e.getStackTrace());
			return flag;
		} finally {
			try {
				psmt.close();

				conn.close();
				return flag;
			} catch (SQLException e) {
				log.error("Error in check dnid method  " + e.getMessage() + " " + e.getStackTrace());
			}

		}
		return true;

	}

	public Timestamp xmlGregorianCalenderToTimestamp(XMLGregorianCalendar xgc) {
		return new Timestamp(xgc.toGregorianCalendar().getTimeInMillis());
	}

	/**
	 * 
	 * This method gets csp_id from asp_csp_txn
	 * 
	 * @param This method takes dnid and seid input.
	 * @return XMLGregorianCalendar
	 */

	public String getcspid(Integer dnid, Integer seid) {

		final String getDcIdquery = "SELECT csp_txn_id FROM public.asp_csp_txn WHERE dnid_no=? and seid=? order by timestamp desc limit 1;";
		String id = null;
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(getDcIdquery)) {
			stmt.setInt(1, dnid);
			stmt.setInt(2, seid);
			log.info("query =" + stmt.toString());
			try (ResultSet rs = stmt.executeQuery();) {

				if (rs.next())
					id = rs.getString(1);
				log.info("id is " + id);
			}
		} catch (SQLException e) {
			log.error("Error in fetching cspid  " + e.getMessage() + " " + e.getStackTrace());

		}

		// log.info("source_id="+id);
		return id;

	}

	/**
	 * 
	 * This method gets global properties
	 * 
	 * @param This method takes key as input.
	 * @return string
	 */

	public String getGolbalProperties(String key) {
		Properties prop = new Properties();
		try {
			InputStream is = getClass().getClassLoader().getResourceAsStream("global.properties");
			prop.load(is);
		} catch (IOException e) {

			log.error("Error in fetching global properties");
		}
		return prop.getProperty(key);
	}

	/**
	 * 
	 * This method sets next_timestamp in dc_position_request table
	 * 
	 * @param This method takes message_id and timestamp as input.
	 * @return void
	 */

	public void setposReqTime(Timestamp instant, String message_id) {

		log.info("updating DC Position request" + message_id);
		String updateDC = "UPDATE public.dc_position_request SET next_timestamp=? WHERE message_id=?;";
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(updateDC)) {
			stmt.setObject(1, instant);
			stmt.setString(2, message_id);
			stmt.executeUpdate();

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();

		}
	}

	public String getowner_login_id(String imoNumber) {
		// TODO Auto-generated method stub
		log.info("getting OWNER LOGIN ID [" + imoNumber + "]");
		final String getowner_companycode = "SELECT owner_loginid FROM public.portal_vessel_details WHERE  imo_no=? ;";
		String id = null;
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(getowner_companycode)) {
			stmt.setString(1, imoNumber);
			ResultSet rs = stmt.executeQuery();

			rs.next();
			id = rs.getString(1);
			/*
			 * while(rs.next()) { id = rs.getString("source_id"); }
			 */

		} catch (SQLException e) {
			e.printStackTrace();

		}

		log.info("[ " + id + "] REquired ID got from Componenet table  ");
		return id;
	}

	public String getloginid(String companyCode) {
		// TODO Auto-generated method stub
		final String getloginid = "SELECT  login_id FROM public.portal_shipping_company WHERE company_code=?;";
		String id = null;
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(getloginid)) {
			stmt.setString(1, companyCode);
			ResultSet rs = stmt.executeQuery();

			rs.next();
			id = rs.getString(1);
			/*
			 * while(rs.next()) { id = rs.getString("source_id"); }
			 */

		} catch (SQLException e) {
			e.printStackTrace();

		}

		log.info("[ " + id + "] login_id from portal shipping Company  ");
		return id;

	}

	public String getlrit_id(String loginId) {
		// TODO Auto-generated method stub
		final String getlritid = "SELECT  requestors_lrit_id FROM public.portal_users Where login_id=?;";
		String id = null;
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(getlritid)) {
			stmt.setString(1, loginId);
			ResultSet rs = stmt.executeQuery();

			rs.next();
			id = rs.getString(1);
			/*
			 * while(rs.next()) { id = rs.getString("source_id"); }
			 */

		} catch (SQLException e) {
			e.printStackTrace();

		}

		log.info("[ " + id + "] lrit_id from portal_users ");
		return id;
	}

	public String[] getIpAddresses(Exchange exchange) throws UnknownHostException {
		try {
			log.info("full header" + exchange.getIn().getHeaders());
			String[] arr = new String[2];
			InetAddress addr = InetAddress.getLocalHost();
			arr[0] = addr.getHostAddress();
			org.apache.cxf.message.Message cxfMessage = exchange.getIn().getHeader(CxfConstants.CAMEL_CXF_MESSAGE,
					org.apache.cxf.message.Message.class);
			// org.apache.cxf.message.Message cxfMessage =
			// exchange.getIn().getHeader(CxfConstants.CAMEL_CXF_MESSAGE,
			// org.apache.cxf.message.Message.class);
			if (cxfMessage != null) {
				HttpServletRequest request = (HttpServletRequest) cxfMessage.get("HTTP.REQUEST");

				arr[1] = request.getHeader("X-Forwarded-For");// arr[1]=0.5.25.170 //arr[1]=null
				log.info("xforwadedfor:  " + arr[1]);
				if (arr[1] == null) {

					arr[1] = request.getRemoteAddr();
					log.info("data in arr[1]" + arr[1]);
				}

			}

			else {
				log.info("Unable to reteive remote address of client, so setting it to LocalHost Addr");
				arr[1] = addr.getHostAddress();
			}
			return arr;
		} catch (UnknownHostException uKH) {
			uKH.printStackTrace();
			log.error("UnknownHostException : Unable to retrieve InetAddress of localhost. ");
			String[] hostAndSender = new String[2];
			hostAndSender[0] = "0.0.0.0";
			hostAndSender[1] = "0.0.0.0";
			return hostAndSender;
		}
	}

	/**
	 * 
	 * This method inserts in ASP_DC_MESSAGE
	 * 
	 * @param This method takes messageid,ipaddresses nodeid,response,payload as
	 *             input.
	 * @return void
	 */
	public void insertASPDCMeassage(String message_id, String marshallDnidRequest, Timestamp currentTimestamp,
			String response, Timestamp responseTimestamp, String ipAddresses, String nodeid) {

		log.info("[" + message_id + "] In DC Message ");
		Connection conn = null;
		PreparedStatement psmt = null;
		String dcMessageQuery = "Insert into public.asp_dc_message(message_id , payload,timestamp,response,response_timestamp,ip_address,node_ip)VALUES(?,?,?,?,?,?,?);";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(dcMessageQuery);
			psmt.setString(1, message_id);
			psmt.setString(2, marshallDnidRequest);
			psmt.setTimestamp(3, currentTimestamp);
			psmt.setString(4, response);
			psmt.setTimestamp(5, responseTimestamp);
			psmt.setString(6, ipAddresses);
			psmt.setString(7, nodeid);
			psmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Error in inserting asp_dc_message  " + e.getMessage() + " " + e.getStackTrace());

		} finally {
			try {
				psmt.close();

				conn.close();
			} catch (SQLException e) {
				log.error("Error in inserting asp_dc_mapping  " + e.getMessage() + " " + e.getStackTrace());
			}

		}

	}

	/**
	 * 
	 * This method updates asp_dc_message
	 * 
	 * @param This method takes response,messageid and timestamp as input
	 * @return void
	 */

	public void updateASPDCMessage(String message_id, String extractResponse, Timestamp timestamp) {

		log.info("[" + message_id + "] Updating Response in  DC Message ");
		String updateDCMessage = "UPDATE public.asp_dc_message SET response=? ,response_timestamp=? WHERE message_id=?;";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(updateDCMessage)) {
			stmt.setString(1, extractResponse);
			stmt.setTimestamp(2, timestamp);
			stmt.setString(3, message_id);

			stmt.executeUpdate();

		} catch (Exception e) {

			log.error("Error in updating asp_dc_message  " + e.getMessage() + " " + e.getStackTrace());

		}

	}

	public String getimo_num(int memNo, int dnidNo) {

		log.info("[" + dnidNo + "] fetching imo number ");
		String id = null;
		String GetImo_numQuery = "SELECT  imo_no FROM public.portal_ship_equipement WHERE member_no=? AND dnid_no=?";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(GetImo_numQuery)) {
			stmt.setInt(1, memNo);
			stmt.setInt(2, dnidNo);
			try (ResultSet rs = stmt.executeQuery();) {
				if (rs.next())
					id = rs.getString(1);

			}
		} catch (SQLException e) {

			log.error("error in fetch imo number" + e.getMessage() + e.getStackTrace());

		}

		log.info("[ " + id + "] Imo_Number ");
		return id;

	}

	/**
	 * 
	 * This method sets status in shipEquiment table
	 * 
	 * @param This method takes dnid,status_date,member_no and status as input.
	 * @return void
	 */

	public void setStatusShipEquipment(int memNo, int Dnidno, String status, Timestamp currentTimestamp) {

		log.info("[" + Dnidno + "] Updating Status in ShipEquipment For DNID  ");
		String updateStatus = "UPDATE public.portal_ship_equipement SET  dnid_status=?, dnid_status_date=? WHERE  member_no=? AND dnid_no=? ;";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(updateStatus)) {
			stmt.setString(1, status);
			stmt.setTimestamp(2, currentTimestamp);
			stmt.setInt(3, memNo);
			stmt.setInt(4, Dnidno);
			stmt.executeUpdate();
		} catch (Exception e) {

			log.error(
					"Error in Updating Status in ShipEquipment For DNID  " + e.getMessage() + " " + e.getStackTrace());
		}
	}

	/**
	 * 
	 * This method updates status in vessel_detail table
	 * 
	 * @param This method takes imo,status,currenttimestamp as input.
	 * @return void
	 */
	public void setstatusVesselDetails(String imo_no, String status, Timestamp currentTimestamp, String oldstatus) {

		log.info("[" + imo_no + "] Updating Status in Vessel Detail For DNID ");
		String updateStatus = "UPDATE public.portal_vessel_details SET   registration_status=?,reg_status_date=? WHERE imo_no=? and registration_status = ?;";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(updateStatus)) {
			stmt.setString(1, status);
			stmt.setTimestamp(2, currentTimestamp);
			stmt.setString(3, imo_no);
			stmt.setString(4, oldstatus);
			stmt.executeUpdate();
		} catch (Exception e) {
			log.error("Error in Updating Status in vessel detail  " + e.getMessage() + " " + e.getStackTrace());

		}
	}

	public void setportalVesselDetails(String imo_no, String status, Timestamp currentTimestamp) {

		log.info("[" + imo_no + "] Updating Status in Vessel Detail For DNID ");
		String updateStatus = "UPDATE public.portal_vessel_details SET   registration_status=?,reg_status_date=? WHERE imo_no=? and registration_status in('SHIP_NOT_REGISTERED','DNID_DOWNLOADED');";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(updateStatus)) {
			stmt.setString(1, status);
			stmt.setTimestamp(2, currentTimestamp);
			stmt.setString(3, imo_no);

			stmt.executeUpdate();
		} catch (Exception e) {
			log.error("Error in Updating Status in vessel detail  " + e.getMessage() + " " + e.getStackTrace());

		}
	}

	/**
	 * 
	 * This method insert into asp_receipt_txn
	 * 
	 * @param This method takesmessagetype,messageid,receiptcode,referenceid as
	 *             input.
	 * @return void
	 */
	public void generatereceipt(String message_id, BigInteger messageType, Timestamp currentTimestamp, int receiptcode,
			String message, String referenceid) {

		log.info("[" + message_id + "] In Receipt Table");
		Connection conn = null;
		PreparedStatement psmt = null;
		String insertReceipt = "INSERT INTO public.asp_receipt_txn( message_type, message_id, timestamp, receipt_code, message,  reference_id) VALUES (?, ?, ?, ?, ?, ?);";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(insertReceipt);
			psmt.setInt(1, messageType.intValue());
			psmt.setString(2, message_id);
			psmt.setTimestamp(3, currentTimestamp);
			psmt.setInt(4, receiptcode);
			psmt.setString(5, message);
			psmt.setString(6, referenceid);
			psmt.executeUpdate();

		} catch (SQLException e) {
			log.error("Error in inserting asp_receipt transaction   " + e.getMessage() + " " + e.getStackTrace());
		} finally {
			try {
				psmt.close();

				conn.close();
			} catch (SQLException e) {
				log.error("Error in Updating Status in ShipEquipment For DNID  " + e.getMessage() + " "
						+ e.getStackTrace());
			}

		}
	}

	/**
	 * 
	 * This method checks if the PTP is sent or not in ASP_CSP_TXN table
	 * 
	 * @param This method takes referencecspid and commandtype input.
	 * @return XMLGregorianCalendar
	 */
	public boolean checkOnetimePOllSent(String referenceCspId, int i) {

		log.info("Checking one time poll already sent or not");
		Connection conn = null;
		PreparedStatement psmt = null;
		boolean flag = false;
		String checkDnid = "SELECT count(*) FROM public.asp_csp_txn where  reference_cspid=? and command_type=?";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(checkDnid);
			psmt.setString(1, referenceCspId);
			psmt.setInt(2, i);
			try (ResultSet rs = psmt.executeQuery();) {
				rs.next();
				int n = rs.getInt(1);
				if (n == 1) {
					log.info("n=" + n);
					log.info("OTP sent already");
					flag = true;
				} else
					flag = false;
			}
		} catch (SQLException e) {

			log.error("Error inchecking otp sent or not  " + e.getMessage() + " " + e.getStackTrace());
			return flag;
		} finally {
			try {
				psmt.close();

				conn.close();
				return flag;
			} catch (SQLException e) {
				log.error("Error inchecking otp sent or not  " + e.getMessage() + " " + e.getStackTrace());
			}

		}
		return true;

	}

	/**
	 * 
	 * This method gets member_no from portal_ship_euipment
	 * 
	 * @param This method takes seid input.
	 * @return member_no
	 */
	public int fetchmem(String shipBorneEquipmentId2) {
		log.info("[" + shipBorneEquipmentId2 + "] fetching mem number ");
		int id = 0;
		String GetImo_numQuery = "SELECT  member_no FROM public.portal_ship_equipement WHERE shipborne_equipment_id=?";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(GetImo_numQuery)) {
			stmt.setString(1, shipBorneEquipmentId2);

			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				id = rs.getInt(1);

		} catch (SQLException e) {
			log.error("Error fetching member_no  " + e.getMessage() + " " + e.getStackTrace());

		}

		log.info("[ " + id + "] member number ");
		return id;

	}

	/**
	 * 
	 * This method gets request_type from position_request_table
	 * 
	 * @param This method takes seid input.
	 * @return XMLGregorianCalendar
	 */

	public int checkrequestType(String shipBorneEquipmentId2) {

		log.info("[" + shipBorneEquipmentId2 + "] fetching request type ");
		int id = 0;
		String GetReqType = "SELECT  request_type FROM public.asp_position_request WHERE shipborne_equipment_id = ? and status = ?";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(GetReqType)) {
			stmt.setString(1, shipBorneEquipmentId2);
			stmt.setString(2, "ACTIVE");
			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				id = rs.getInt(1);

		} catch (SQLException e) {

			log.error("Error in fetching request type" + e.getMessage() + e.getStackTrace());

		}

		log.info("[ " + id + "] requestType ");
		return id;
	}

	/**
	 * 
	 * This method fetches next_timestamp from dc_position_request
	 * 
	 * @param This method takes no input.
	 * @return next_timestamp
	 */

	public Timestamp fetchstartTime(String messageID) {

		log.info("[" + messageID + "] fetching start time ");
		Timestamp id = null;
		String GetReqType = "SELECT  next_timestamp FROM public.dc_position_request where message_id=?;";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(GetReqType)) {
			stmt.setString(1, messageID);

			try (ResultSet rs = stmt.executeQuery();) {
				if (rs.next())
					id = rs.getTimestamp(1);

			}
		} catch (SQLException e) {

			log.error("Error in fetching start time " + e.getMessage() + "  " + e.getStackTrace());

		}

		log.info("[ " + id + "] startTime ");
		return id;

	}

	/**
	 * 
	 * This method gets frequency from asp_terminal_frequency
	 * 
	 * @param This method takes dnid and mem_no as input.
	 * @return frequency
	 */
	public int getfrequency(int dnidno2, int mem_no) {

		log.info("[" + dnidno2 + "] fetching frequencyrate of existing ACTIVE Request  ");
		int id = 0;
		String Getfrequency = "SELECT  frequency_rate FROM public.asp_terminal_frequency WHERE dnid_no= ? and member_no = ? and status=? ";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(Getfrequency)) {
			stmt.setInt(1, Integer.valueOf(dnidno2));
			stmt.setInt(2, Integer.valueOf(mem_no));
			stmt.setString(3, "ACTIVE");
			// stmt.setString(4, dataUserRequestor);
			try (ResultSet rs = stmt.executeQuery();) {
				if (rs.next())
					id = rs.getInt(1);

			}
		} catch (SQLException e) {

			log.error("Fetching frequency ::" + e.getMessage() + e.getStackTrace());

		}

		log.info("[ " + id + "] requestType ");
		return id;

	}

	/**
	 * 
	 * This method sets start time in asp_terminal_frequency
	 * 
	 * @param This method takes timestamp, dnid ,mem_no as input.
	 * @return void
	 */

	public void setTimeFrequency(int dnid, int member_no, Timestamp next) {

		log.info("updating Start Time in Frequency Table after Sync " + "[" + dnid + "]" + "[" + member_no + "]");
		final String setTimestamp = "UPDATE public.asp_terminal_frequency SET starttime=? Where dnid_no=? and  member_no=? and status=?;";
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(setTimestamp)) {
			stmt.setTimestamp(1, next);
			stmt.setInt(2, dnid);
			stmt.setInt(3, member_no);
			stmt.setString(4, "ACTIVE");
			stmt.executeUpdate();
		} catch (Exception e) {
			log.error("Error in setting time frequency  " + e.getMessage() + " " + e.getStackTrace());

		}

	}

	/**
	 * 
	 * This methodchecks if we have received PDN for stop command or not
	 * 
	 * @param This method takes dnid,referencecspid and member_no input.
	 * @return boolean
	 */

	public boolean checkIfStopPDNReceived(String dnidno2, String referenceCspId, Integer mem) {

		log.info("checking if set request Already sent");
		Connection conn = null;
		PreparedStatement psmt = null;
		boolean flag = false;
		String checkfromASPquery = "SELECT count(*) FROM public.asp_csp_txn where  command_type=? and dnid_no=? and member_no =? and reference_cspid=?";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(checkfromASPquery);

			psmt.setInt(1, 4);
			psmt.setInt(2, Integer.parseInt(dnidno2));
			psmt.setInt(3, mem);
			psmt.setString(4, referenceCspId);
			log.info("query=" + psmt.toString());
			try (ResultSet rs = psmt.executeQuery();) {
				rs.next();
				int n = rs.getInt(1);
				if (n != 0) {
					// log.info("result set has got something");
					log.info("n =" + n);
					flag = true;
				} else
					flag = false;
			}
		} catch (SQLException e) {

			log.error("Error inchecking if stop pnd received  " + e.getMessage() + " " + e.getStackTrace());
			return flag;
		} finally {
			try {
				psmt.close();

				conn.close();
				return flag;
			} catch (SQLException e) {
				log.error("Error inchecking if stop pnd received  " + e.getMessage() + " " + e.getStackTrace());
			}

		}
		return true;

	}

	/**
	 * 
	 * This method checks if the start command is already sent or not
	 * 
	 * @param This method takes referencecspid as input.
	 * @return boolean
	 */
	public boolean checkStartsent(String referenceCspId) {

		log.info("checking if start  request Already sent");
		Connection conn = null;
		PreparedStatement psmt = null;
		boolean flag = false;
		String checkfromASPquery = "SELECT count(*) FROM public.asp_csp_txn where  command_type=? and reference_cspid=?";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(checkfromASPquery);

			psmt.setInt(1, 5);

			psmt.setString(2, referenceCspId);
			log.info("query=" + psmt.toString());
			try (ResultSet rs = psmt.executeQuery();) {
				rs.next();
				int n = rs.getInt(1);
				if (n != 0) {
					// log.info("result set has got something");
					log.info("n =" + n);
					flag = true;
				} else
					flag = false;
			}
		} catch (SQLException e) {

			log.error("Error inchecking if start  already sent  " + e.getMessage() + " " + e.getStackTrace());
			return flag;
		} finally {
			try {
				psmt.close();

				conn.close();
				return flag;
			} catch (SQLException e) {
				log.error("Error inchecking if start  already sent  " + e.getMessage() + " " + e.getStackTrace());
			}

		}
		return true;

	}

	/**
	 * 
	 * This method checks vessel status from portal_vessel_details
	 * 
	 * @param This method takes imoNumber as input.
	 * @return boolean
	 */
	public boolean checkvesselstatus(String imoNumber) {

		log.info("checking if DNID downloaded or not");
		Connection conn = null;
		PreparedStatement psmt = null;
		boolean flag = false;
		String statusquery = "select count(*) from public.portal_vessel_details where imo_no =? and registration_status =? ";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(statusquery);
			psmt.setString(1, imoNumber);
			psmt.setString(2, "DNID_DOWNLOADED");
			log.info("query=" + psmt.toString());
			try (ResultSet rs = psmt.executeQuery();) {
				rs.next();
				int n = rs.getInt(1);
				if (n != 0) {
					// log.info("result set has got something");
					log.info("n =" + n);
					flag = true;
				} else
					flag = false;
			}
		} catch (SQLException e) {

			log.error("Error in checking dnid downloaded or not" + e.getMessage() + " " + e.getStackTrace() + " "
					+ e.getStackTrace());
			return flag;
		} finally {
			try {
				psmt.close();

				conn.close();
				return flag;
			} catch (SQLException e) {
				log.error("Error in checking dnid downloaded or not" + e.getMessage() + " " + e.getStackTrace() + " "
						+ e.getStackTrace());
			}

		}
		return true;

	}

	public boolean checkportalstatus(String imoNumber) {

		log.info("checking if DNID downloaded or not");
		Connection conn = null;
		PreparedStatement psmt = null;
		boolean flag = false;
		String statusquery = "select count(*) from public.portal_vessel_details where imo_no =? and registration_status in ('SHIP_NOT_REGISTERED','DNID_DOWNLOADED') ";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(statusquery);
			psmt.setString(1, imoNumber);
			// psmt.setString(2, "DNID_DOWNLOADED");
			log.info("query=" + psmt.toString());
			ResultSet rs = psmt.executeQuery();
			rs.next();
			int n = rs.getInt(1);
			if (n != 0) {
				// log.info("result set has got something");
				log.info("n =" + n);
				flag = true;
			} else
				flag = false;
		} catch (SQLException e) {

			log.error("Error in checking dnid downloaded or not" + e.getMessage() + " " + e.getStackTrace() + " "
					+ e.getStackTrace());
			return flag;
		} finally {
			try {
				psmt.close();

				conn.close();
				return flag;
			} catch (SQLException e) {
				log.error("Error in checking dnid downloaded or not" + e.getMessage() + " " + e.getStackTrace() + " "
						+ e.getStackTrace());
			}

		}
		return true;

	}

	/**
	 * 
	 * This method calls alert Procedure
	 * 
	 * @param This method takes seid and alertid as input.
	 * @return boolean
	 */
	public boolean generateAlert(String Seid, int alertid) {
		log.info("******************calling alert Procedure*************");
		try (Connection con = dataSource.getConnection();
				PreparedStatement stmt = con.prepareStatement("call  portal_alert_transaction_ASP(?,?)");) {
			stmt.setString(1, Seid);
			stmt.setInt(2, alertid);
			stmt.execute();
			log.info("updated alert Table");
			return true;
		} catch (SQLException e) {
			log.error("Error in adding alert " + e.getMessage() + " " + e.getStackTrace());

			return false;
		}
	}

	public boolean generateAlert(String dnidno, String memberno, int alertid) {
		log.info("******************calling alert Procedure*************");
		try (Connection con = dataSource.getConnection();
				PreparedStatement stmt = con.prepareStatement("call procedure_for_position_report_not_found(?,?,?)");) {
			stmt.setString(1, dnidno);
			stmt.setString(2, memberno);
			stmt.setInt(3, alertid);
			stmt.execute();
			log.info("updated alert Table");
			return true;
		} catch (SQLException e) {
			log.error("Error in adding alert " + e.getMessage() + " " + e.getStackTrace());

			return false;
		}
	}

	/**
	 * 
	 * This method terminates all request for particular imo in asp_frequency table
	 * 
	 * @param This method takes imo input.
	 * @return void
	 */

	public void terminateallRequest(String imo_no) {

		log.info("deleting all request in terminal Frequency Table against IMO [" + imo_no + "]");
		Connection conn = null;
		PreparedStatement psmt = null;
		String deleteQuery = "DELETE FROM public.asp_terminal_frequency  WHERE imo_no=?;";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(deleteQuery);
			psmt.setInt(1, Integer.valueOf(imo_no));
			log.info("query=" + psmt.toString());
			psmt.executeUpdate();
		} catch (Exception e) {

			log.error("Error in terminating all request" + e.getMessage() + " " + e.getStackTrace());

		} finally {
			try {
				psmt.close();

				conn.close();
			} catch (SQLException e) {
				log.error("Error in terminating all request " + e.getMessage() + " " + e.getStackTrace() + " ");
			}

		}

	}

	/**
	 * 
	 * This method gets requestor from asp_terminal_frequency
	 * 
	 * @param This method takes imo_no and status input.
	 * @return cglrit_id
	 */

	public String fetchrequestor(String imo_no, String status) {

		log.info("[" + imo_no + "] fetching Cg_lrit_id with maximum frequecy rate for this IMO  ");
		String id = null;
		String Getcgid = "select cg_lrit_id from asp_terminal_frequency where imo_no= ?  and status =? ";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(Getcgid)) {
			stmt.setInt(1, Integer.valueOf(imo_no));

			stmt.setString(2, status);
			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				id = rs.getString(1);
			log.info("prerequestor=" + id);

		} catch (SQLException e) {

			log.error("fetching requestor from terminal frequency " + e.getMessage() + " " + e.getStackTrace());

		}

		log.info("[ " + id + "] cglritid ");
		return id;

	}

	/**
	 * 
	 * This method sets status in aspterminal_frequency
	 * 
	 * @param This method takes imo,status and prerequestor as input.
	 * @return void
	 */
	public void setstatusTerminal(String preRequestor, String imo_no, String Status) {

		log.info("Setting status Inactive");
		String InactiveQuery = "UPDATE public.asp_terminal_frequency SET status=? WHERE imo_no=? and cg_lrit_id=?;";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(InactiveQuery)) {
			stmt.setString(1, Status);
			stmt.setInt(2, Integer.valueOf(imo_no));
			stmt.setString(3, preRequestor);
			stmt.executeUpdate();
			log.info("Status changed");
		} catch (Exception e) {

			log.error("Error in changing Status " + e.getMessage() + e.getStackTrace());
		}
	}

	/**
	 * 
	 * This method gets status from asp_terminal_frequency
	 * 
	 * @param This method takes cglrit_id and imo input.
	 * @return staus
	 */
	public String checkTerminalstatus(String dataUserRequestor, String imo_no) {

		log.info("fetching  status of exixting dataUserrequestor and imo_no");
		String Fetchstatus = "SELECT status FROM public.asp_terminal_frequency where imo_no=? and cg_lrit_id =? ;";
		String id = null;
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(Fetchstatus)) {

			stmt.setInt(1, Integer.valueOf(imo_no));
			stmt.setString(2, dataUserRequestor);
			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				id = rs.getString(1);

		} catch (SQLException e) {

			log.error("fetching status from Terminal Frequency " + e.getMessage() + " " + e.getStackTrace());

		}

		log.info("[ " + id + "] status of exixting request ");
		return id;

	}

	/**
	 * 
	 * This method gets minimum frequency for a particular imo
	 * 
	 * @param This method takes imo_no input.
	 * @return frequency
	 */
	public int getminimumfrequency(String imo_no, String string) {

		log.info("fetching  minimum frequency for IMO");
		String Fetchstatus = "select frequency_rate from public.asp_terminal_frequency where  asp_terminal_frequency.imo_no=? order by asp_terminal_frequency.frequency_rate limit 1";

		int frequency = 0;
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(Fetchstatus)) {

			stmt.setInt(1, Integer.valueOf(imo_no));
			// stmt.setString(2, dataUserRequestor);
			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				frequency = rs.getInt(1);

		} catch (SQLException e) {

			log.error("fetching minimum Terminal frequency for IMO " + e.getMessage() + " " + e.getStackTrace());

		}

		log.info("[ " + frequency + "] status of exixting request ");
		return frequency;
	}

	/**
	 * 
	 * This method gets messageid from asp_dc_csp_mapping
	 * 
	 * @param This method takes cspid as input.
	 * @return messageid
	 */

	public String fetchMessageID(String referenceCspId) {

		log.info("fetching Message ID for IMO");
		String FetchmessageID = "select message_id from asp_dc_csp_mapping_txn where csp_id=?";

		String frequency = null;
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(FetchmessageID)) {

			stmt.setString(1, referenceCspId);
			// stmt.setString(2, dataUserRequestor);
			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				frequency = rs.getString(1);

		} catch (SQLException e) {

			log.error("fetching message ID from mapping table " + e.getMessage() + " " + e.getStackTrace());

		}

		log.info("[ " + frequency + "] messageID");
		return frequency;

	}

	/**
	 * 
	 * This method checks if imo_no entry is present in asp_frequency table
	 * 
	 * @param This method takes imo_no input.
	 * @return int
	 */

	public int checkImoPresent(String imo_no) {

		log.info("checking if IMO entry present in Terminal Frequency Table");
		Connection conn = null;
		PreparedStatement psmt = null;
		boolean flag = false;
		int n = 0;
		String statusquery = "select count(*) from public.asp_terminal_frequency where imo_no =?";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(statusquery);
			psmt.setInt(1, Integer.valueOf(imo_no));
			// psmt.setString(2,"DNID_DOWNLOADED");
			log.info("query=" + psmt.toString());
			ResultSet rs = psmt.executeQuery();
			rs.next();
			n = rs.getInt(1);
			if (n != 0) {
				// log.info("result set has got something");
				log.info("n =" + n);
				flag = true;
			} else
				flag = false;
		} catch (SQLException e) {

			log.error("Error in checking Terminal Frequency Table " + e.getMessage() + " " + e.getStackTrace() + " "
					+ e.getStackTrace());
			return n;
		} finally {
			try {
				psmt.close();

				conn.close();
				return n;
			} catch (SQLException e) {
				log.error(
						"Error in checking Terminal Frequency Table " + e.getMessage() + " " + e.getStackTrace() + " ");
			}

		}

		return n;

	}

	public String fetchSEID(String imo_no) {

		log.info("[" + imo_no + "] fetching SEID with maximum frequecy rate for this IMO  ");
		String seid = null;

		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(FetchSEID)) {
			stmt.setString(1, imo_no);

			log.info(stmt.toString());

			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				seid = rs.getString(1);

		} catch (SQLException e) {

			log.error("failed in fetching seid :" + e.getMessage() + " " + e.getStackTrace());

		}

		log.info("[ " + seid + "] SEID , IMO : " + imo_no);
		return seid;

	}

	public String[] getrandomposition(String imo) {
		log.info("Get Random position for ship imo: " + imo);
		Connection conn = null;
		PreparedStatement psmt = null;
		String[] position = new String[2];
		int random_number = (int) (Math.random() * 3) + 1;
		String FetchPosition = "select latitude, longitude from public.asp_temp_position where imo_no=? and random_id=?";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(FetchPosition);
			psmt.setString(1, imo);
			psmt.setInt(2, random_number);
			log.info("query for fetching random position for imo_no: " + "[ " + imo + " ] .Query : " + psmt.toString());
			ResultSet rs = psmt.executeQuery();

			if (rs.next() == false) {
				log.info("random position ResultSet is empty");
				position[0] = "18.57.00.N";
				position[1] = "072.56.60.E";
			} else {
				do {
					position[0] = rs.getString(1);
					position[1] = rs.getString(2);
					log.info("Lat & Long picked in do_while******: [ " + position[0] + " : " + position[1] + " ]");
				} while (rs.next());
			}
			log.info("Lat & Long picked : [ " + position[0] + " : " + position[1] + " ]");

		} catch (SQLException e) {
			e.printStackTrace();
		}

		finally {
			try {
				psmt.close();
				conn.close();
				return position;

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return position;

	}

	public String fetchcg(Integer imo_no) {

		log.info("[" + imo_no + "] fetching CGID for this IMO  ");
		String cgid = null;

		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(FetchCGID)) {
			stmt.setInt(1, imo_no);

			log.info(stmt.toString());

			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				cgid = rs.getString(1);

		} catch (SQLException e) {

			log.error("fetching requestor from terminal frequency " + e.getMessage() + " " + e.getStackTrace());

		}

		log.info("[ " + cgid + "] CGID , IMO : " + imo_no);
		return cgid;

	}

	public int fetchfrequecyMinimum(String imo_no) {

		log.info("[" + imo_no + "] fetching CGID for this IMO  ");
		int frequency = 0;

		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(fetchFrequency)) {
			stmt.setInt(1, Integer.valueOf(imo_no));

			log.info(stmt.toString());

			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				frequency = rs.getInt(1);

		} catch (SQLException e) {

			log.error("fetching Minimum frequency from terminal frequency " + e.getMessage() + " " + e.getStackTrace());

		}

		log.info("[ " + frequency + "] Minimum frequency  , IMO : " + imo_no);
		return frequency;
	}

	/**
	 * 
	 * This method fetch cglrit_id
	 * 
	 * @param This method takes imo_no and frequency as input.
	 * @return cglrit_id
	 */

	public String fetchcgfromTerminal(String imo_no, int minimumfrequency) {

		log.info("[" + imo_no + "] fetching CGID for this IMO  ");
		String cg = null;

		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(fetchFrequency)) {
			stmt.setInt(1, minimumfrequency);
			stmt.setInt(2, Integer.valueOf(imo_no));

			log.info(stmt.toString());

			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				cg = rs.getString(1);

		} catch (SQLException e) {

			log.error("fetching Minimum frequency from terminal frequency " + e.getMessage() + " " + e.getStackTrace());

		}

		log.info("[ " + cg + "] Minimum frequency  , IMO : " + imo_no);
		return cg;
	}

	/**
	 * 
	 * This method fetches timedelay from portal_vessel_detail
	 * 
	 * @param This method takes imo_no as input.
	 * @return timedelay
	 */
	public int getDelay(String imo_no) {

		log.info("[" + imo_no + "] fetching Delay for this IMO  ");
		int delay = 0;
		String delayQuery = "select time_delay from portal_vessel_details where imo_no=? and  registration_status != 'SOLD'";
		try (
				// and ((registration_status = 'SHIP_REGISTERED') or
				// (registration_status = 'DNID_DOWNLOADED'))
				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(delayQuery)) {
			stmt.setString(1, imo_no);
			// stmt.setInt(2,Integer.valueOf(imo_no));

			log.info(stmt.toString());

			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				delay = rs.getInt(1);

		} catch (SQLException e) {

			log.error("fetching Delay from portal_vessel_detail " + e.getMessage() + " " + e.getStackTrace());

		}

		log.info("[ " + delay + "]Delay  , IMO : " + imo_no);
		return delay;
		// return 0;
	}

	/**
	 * 
	 * This method fetches message_id
	 * 
	 * @param This method takes imo_no as input.
	 * @return message_id
	 */
	public String getmessageID(String imo_no) {

		log.info("[" + imo_no + "] fetching messageID for this IMO  ");
		String messageID = null;
		String getmessageID = "select message_id from dc_position_report where imo_no = ? and data_user_requestor = ? and message_type = '1' and report_status='active' and csso_flag = 't' order by lrit_timestamp desc limit 1";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(getmessageID)) {
			stmt.setString(1, imo_no);
			stmt.setString(2, "1065");

			// stmt.setInt(2,Integer.valueOf(imo_no));

			log.info(stmt.toString());

			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				messageID = rs.getString(1);

		} catch (SQLException e) {

			log.error("fetching Delay from portal_vessel_detail " + e.getMessage() + " " + e.getStackTrace());

		}
		return messageID;
	}

	/**
	 * 
	 * This method cglrit_id of the owner of the vessel
	 * 
	 * @param This method takes imo_number as input.
	 * @return cglrit_id
	 */
	public String getCgOwnerFromIMONO(String imo_no) {

		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(GET_CGOWNER_FROM_IMO_NO);) {
			stmt.setString(1, imo_no);
			log.info("FIND QUNER QUERY :" + stmt.toString());
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getString(1);
			} else {
				return null;
			}
		} catch (SQLException e) {
			log.error("ERROR FINDIN CG OWNER :  " + imo_no + " " + e.getMessage());
			return null;
		}
	}

	public void deleteDnidMemberNo(String imo_no) {

		String updatePortalShipEquipment = "UPDATE public.portal_ship_equipement SET  dnid_no= NULL , member_no= NULL WHERE imo_no=?";
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(updatePortalShipEquipment);) {
			stmt.setString(1, imo_no);
			stmt.executeUpdate();
			log.info(" QUERY :" + stmt.toString());

		} catch (SQLException e) {
			log.error("Error in updating portal_ship_equipement :  " + imo_no + " " + e.getMessage() + " "
					+ e.getStackTrace());
		}
	}

	/**
	 * 
	 * This method sets status in portal_vessel_detail
	 * 
	 * @param This method takes timestamp and imo_no as input.
	 * @return void
	 */
	public void setstatusDnidDeleted(String imo_no, String string, Timestamp currentTimestamp) {

		log.info("[" + imo_no + "] Updating Status in Vessel Detail For DNID ");
		String updateStatus = "UPDATE public.portal_vessel_details SET   registration_status=?,reg_status_date=? WHERE imo_no=? and ((registration_status = 'SHIP_REGISTERED') or (registration_status = 'DNID_SEID_DEL_REQ') or (registration_status = 'DNID_DEL_REQ'));";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(updateStatus)) {
			stmt.setString(1, string);
			stmt.setTimestamp(2, currentTimestamp);
			stmt.setString(3, imo_no);

			stmt.executeUpdate();
		} catch (Exception e) {

			log.error("Error in setting status DNID DELETED " + e.getMessage() + " " + e.getStackTrace() + " ");

		}

	}

	/**
	 * 
	 * This method fetches start_time
	 * 
	 * @param This method takes imo_number as input.
	 * @return Timestamp
	 */
	public Timestamp fetchTimefrequency(String imo)

	{

		log.info("[" + imo + "] fetching messageID for this IMO  ");
		Timestamp time = null;
		String getTimestamp = "SELECT starttime FROM public.asp_terminal_frequency where imo_no=?;";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(getTimestamp)) {
			stmt.setInt(1, Integer.valueOf(imo));
			// stmt.setString(2, "1065");

			// stmt.setInt(2,Integer.valueOf(imo_no));

			log.info(stmt.toString());

			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				time = rs.getTimestamp(1);

		} catch (SQLException e) {

			log.error("fetching timestamp from Terminal frequency " + e.getMessage() + " " + e.getStackTrace());

		}
		return time;

	}

	/**
	 * 
	 * This method fetches cspid
	 * 
	 * @param This method takes dnidno and mem_number as input.
	 * @return cspid
	 */

	public String getCspIdNew(Integer dnid, Integer member_no) {
		String id = null;
		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(GET_CSP_TXN_ID)) {
			stmt.setInt(1, dnid);
			stmt.setInt(2, member_no);
			log.info("query =" + stmt.toString());
			ResultSet rs = stmt.executeQuery();

			if (rs.next())
				id = rs.getString(1);
			log.info("id is " + id);
		} catch (SQLException e) {
			log.error("Error in fetching csp_txn_id " + e.getMessage() + " " + e.getStackTrace() + " ");
		}
		return id;
	}

	/**
	 * 
	 * This method checks cglrit_id is present in frequency table
	 * 
	 * @param This method takes imo_no and cglrid_id as input.
	 * @return gives a number of entries
	 */
	public int checkCGPresent(String dataUserRequestor, String imo_no) {

		log.info("checking if IMO entry present in Terminal Frequency Table");
		Connection conn = null;
		PreparedStatement psmt = null;
		boolean flag = false;
		int n = 0;
		String statusquery = "select count(*) from public.asp_terminal_frequency where imo_no =? and cg_lrit_id=?";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(statusquery);
			psmt.setInt(1, Integer.valueOf(imo_no));
			psmt.setString(2, dataUserRequestor);
			// psmt.setString(2,"DNID_DOWNLOADED");
			log.info("query=" + psmt.toString());
			ResultSet rs = psmt.executeQuery();
			rs.next();
			n = rs.getInt(1);
			if (n != 0) {
				// log.info("result set has got something");
				log.info("n =" + n);
				flag = true;
			} else
				flag = false;
		} catch (SQLException e) {

			log.error("Error in checking Terminal Frequency Table " + e.getMessage() + " " + e.getStackTrace());
			return n;
		} finally {
			try {
				psmt.close();

				conn.close();
				return n;
			} catch (SQLException e) {
				log.error("Error in checking Terminal Frequency Table " + e.getMessage() + " " + e.getStackTrace());
			}

		}

		return n;

	}

	/**
	 * 
	 * This method checks start DNID pdn
	 * 
	 * @param This method takes dnidno and referencecspid and mem_number as input.
	 * @return boolean
	 */
	public boolean checkIfStartPDNReceived(String dnidno2, String referenceCspId, int memNo) {

		log.info("checking if start pdn Already received");
		Connection conn = null;
		PreparedStatement psmt = null;
		boolean flag = false;
		String checkfromASPquery = "SELECT count(*) FROM public.asp_csp_txn where  command_type=? and dnid_no=? and member_no =? and reference_cspid=?";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(checkfromASPquery);

			psmt.setInt(1, 5);
			psmt.setInt(2, Integer.parseInt(dnidno2));
			psmt.setInt(3, memNo);
			psmt.setString(4, referenceCspId);
			log.info("query=" + psmt.toString());
			ResultSet rs = psmt.executeQuery();
			rs.next();
			int n = rs.getInt(1);
			if (n == 1) {
				// log.info("result set has got something");
				log.info("n =" + n);
				flag = true;
			} else
				flag = false;
		} catch (SQLException e) {

			log.error("Error in checking start pdn received " + e.getMessage() + " " + e.getStackTrace());
			return flag;
		} finally {
			try {
				psmt.close();

				conn.close();
				return flag;
			} catch (SQLException e) {
				log.error("Error in checking start pdn received " + e.getMessage() + " " + e.getStackTrace());
			}

		}
		return true;

	}

	/**
	 * 
	 * This method checks delete DNID pdn
	 * 
	 * @param This method takes dnidno and referencecspid and mem_number as input.
	 * @return boolean
	 */

	public boolean checkIfdeletePDNReceived(String dnidno2, String referenceCspId, int memNo) {

		log.info("checking if delete pdn  Already recieved");
		Connection conn = null;
		PreparedStatement psmt = null;
		boolean flag = false;
		String checkfromASPquery = "SELECT count(*) FROM public.asp_csp_txn where dnid_no=? and member_no =? and reference_cspid=?";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(checkfromASPquery);

			// psmt.setInt(1,11);
			psmt.setInt(1, Integer.parseInt(dnidno2));
			psmt.setInt(2, memNo);
			psmt.setString(3, referenceCspId);
			log.info("query=" + psmt.toString());
			ResultSet rs = psmt.executeQuery();
			rs.next();
			int n = rs.getInt(1);
			if (n == 1) {
				// log.info("result set has got something");
				log.info("n =" + n);
				flag = true;
			} else
				flag = false;
		} catch (SQLException e) {

			log.error("Error in checking delete pdn received " + e.getMessage() + " " + e.getStackTrace());
			return flag;
		} finally {
			try {
				psmt.close();

				conn.close();
				return flag;
			} catch (SQLException e) {
				log.error("Error in checking delete pdn received " + e.getMessage() + " " + e.getStackTrace());
			}

		}
		return true;

	}

	/**
	 * 
	 * This method checks if the pdn is received for a particular command
	 * 
	 * @param This method takes command_type and reference_cspid as input.
	 * @return boolean
	 */

	public boolean checkPdnReceived(String referenceCspId, int commandTypeinMail) {

		Connection conn = null;
		PreparedStatement psmt = null;
		boolean flag = false;
		String query = "select count(*) from public.asp_csp_txn where reference_cspid  =? and command_type=? and message_status='PDN RECEIVED'";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(query);
			psmt.setString(1, referenceCspId);
			psmt.setInt(2, commandTypeinMail);
			log.info("query=" + psmt.toString());
			ResultSet rs = psmt.executeQuery();
			rs.next();
			int n = rs.getInt(1);
			if (n == 1) {

				log.info("n =" + n);
				flag = true;
			} else
				flag = false;
		} catch (SQLException e) {

			log.error("Error in checking  pdn received " + e.getMessage() + " " + e.getStackTrace());
			return flag;
		} finally {
			try {
				psmt.close();

				conn.close();
				return flag;
			} catch (SQLException e) {
				log.error("Error in checking  pdn received " + e.getMessage() + " " + e.getStackTrace());
			}

		}
		return true;

	}

	public boolean checkDownloadPDNReceived(String referenceCspId, int commandTypeinMail) {

		Connection conn = null;
		PreparedStatement psmt = null;
		boolean flag = false;
		String query = "select count(*) from public.asp_csp_txn where csp_txn_id  =? and command_type=? and message_status='PDN RECEIVED'";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(query);
			psmt.setString(1, referenceCspId);
			psmt.setInt(2, commandTypeinMail);
			log.info("query=" + psmt.toString());
			ResultSet rs = psmt.executeQuery();
			rs.next();
			int n = rs.getInt(1);
			if (n == 1) {

				log.info("n =" + n);
				flag = true;
			} else
				flag = false;
		} catch (SQLException e) {

			log.error("Error in checking  pdn received " + e.getMessage() + " " + e.getStackTrace());
			return flag;
		} finally {
			try {
				psmt.close();

				conn.close();
				return flag;
			} catch (SQLException e) {
				log.error("Error in checking  pdn received " + e.getMessage() + " " + e.getStackTrace());
			}

		}
		return true;

	}

	/**
	 * 
	 * This method sets status in asp_csp_txn
	 * 
	 * @param This method takes command_type and reference_cspid as input.
	 * @return XMLGregorianCalendar
	 */

	public void setStatusTxn(String status, String referenceCspId, int commandTypeinMail) {

		final String updateTransaction = "UPDATE public.asp_csp_txn SET message_status=? WHERE reference_cspid  =? and command_type=? ";

		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(updateTransaction)) {
			stmt.setString(1, status);
			stmt.setString(2, referenceCspId);
			stmt.setInt(3, commandTypeinMail);
			log.info("query=" + stmt.toString());
			stmt.executeUpdate();

		}

		catch (Exception e) {
			log.error("Error in setting status in asp_csp_txn " + e.getMessage() + " " + e.getStackTrace());

		}

	}

	public String fetchmodelid(String seid) {
		log.info("[" + seid + "] fetching model id   ");
		String model_id = null;
		String getcommandtype = "select  model_id from portal_ship_equipement  where shipborne_equipment_id =?";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(getcommandtype)) {
			stmt.setString(1, seid.trim());

			log.info(stmt.toString());

			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				model_id = rs.getString("model_id");

		} catch (SQLException e) {

			log.error("fetching modeltype  " + e.getMessage() + " " + e.getStackTrace());

		}
		return model_id;
	}

	public String fetchmodeltype(String model_id) {

		log.info("[" + model_id + "] fetching model type  ");
		BigInteger model = new BigInteger(model_id);
		String modeltype = null;
		String getcommandtype = "select model_type from portal_model_details  where model_id =?";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(getcommandtype)) {
			stmt.setObject(1, model, Types.BIGINT);
			log.info(stmt.toString());

			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				modeltype = rs.getString("model_type");

		} catch (SQLException e) {

			log.error("fetching modeltype  " + e.getMessage() + " " + e.getStackTrace());

		}
		return modeltype;
	}

	public String fetchshipstatus(String imoNumber) {

		log.info("[" + imoNumber + "] fetching ship status  ");
		String status = null;
		String getcommandtype = "select registration_status from portal_vessel_details  where imo_no  =?";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(getcommandtype)) {
			stmt.setString(1, imoNumber.trim());

			log.info(stmt.toString());

			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				status = rs.getString("registration_status");

		} catch (SQLException e) {

			log.error("fetching registration_status " + e.getMessage() + " " + e.getStackTrace());

		}
		return status;
	}

	/**
	 * 
	 * This method fetches command type
	 * 
	 * @param This method takes reference_cspid as input.
	 * 
	 * @return command type
	 */

	public int fetchCommandtype(String referenceCspId) {
		log.info("[" + referenceCspId + "] fetching Root command  type   ");
		int commandtype = 0;
		String getcommandtype = "SELECT command_type FROM public.asp_csp_txn where csp_txn_id=?";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(getcommandtype)) {
			stmt.setString(1, referenceCspId);

			log.info(stmt.toString());

			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				commandtype = rs.getInt("command_type");

		} catch (SQLException e) {
			log.error("fetching Delay from portal_vessel_detail " + e.getMessage() + " " + e.getStackTrace());

		}
		return commandtype;
	}

	/**
	 * 
	 * This method set is_active as false for all previous dnid requests for this
	 * ship
	 * 
	 * @param This method takes imo_no as input.
	 * 
	 * @return void
	 */

	public void setDnidInactive(String imoNo) {

		final String updateTransaction = "UPDATE public.asp_dnid_txn SET is_active = ?  where imo_no= ?";

		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(updateTransaction)) {
			stmt.setBoolean(1, false);
			stmt.setString(2, imoNo);

			stmt.executeUpdate();

		} catch (Exception e) {
			log.error("Error in updating asp_dnid_db " + e.getMessage() + " " + e.getStackTrace());

		}

	}

	public String fetchPortalStatus(String imo_no) {
		log.info("checking for portal status");
		Connection conn = null;
		PreparedStatement psmt = null;
		String strr = "";
		String status = "select registration_status  from public.portal_vessel_details where imo_no =? and registration_status !='SOLD' ";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(status);
			psmt.setString(1, imo_no);

			log.info("query=" + psmt.toString());
			ResultSet rs = psmt.executeQuery();
			rs.next();
			strr = rs.getString(1);
			log.info("status is " + strr);
		} catch (SQLException e) {

			log.error("Error in fetching status from portal vessel table" + e.getMessage() + " " + e.getStackTrace()
					+ " " + e.getStackTrace());
			return strr;
		} finally {
			try {
				psmt.close();

				conn.close();
				return strr;
			} catch (SQLException e) {
				log.error("Error in fetching status from portal vessel table" + e.getMessage() + " " + e.getStackTrace()
						+ " " + e.getStackTrace());
			}

		}
		return strr;
	}

	public void deleteSEID_DNID(String imo_no) {
		log.info("checking for portal status");
		Connection conn = null;
		PreparedStatement psmt = null;
		String strr = "";
		String deleteQuery = "delete FROM public.portal_ship_equipement where imo_no=?;";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(deleteQuery);
			psmt.setString(1, imo_no);

			log.info("query=" + psmt.toString());
			psmt.executeUpdate();
		} catch (SQLException e) {

			log.error("Error in deleting row from ship_equipment table" + e.getMessage() + " " + e.getStackTrace() + " "
					+ e.getStackTrace());
		} finally {
			try {
				psmt.close();

				conn.close();
			} catch (SQLException e) {
				log.error("Error in deleting row from ship_equipment table" + e.getMessage() + " " + e.getStackTrace()
						+ " " + e.getStackTrace());
			}

		}
	}

	public String insertApiDashboard(String request) {

		Connection conn = null;
		PreparedStatement psmt = null;
		String rowno = "";
		String insertQuery = "INSERT INTO public.asp_api_dashboard(request, req_timestamp) VALUES (?, ?) RETURNING id;";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(insertQuery);
			psmt.setString(1, request);
			psmt.setTimestamp(2, getCurrentTimestamp());

			log.info("query=" + psmt.toString());
			// psmt.executeUpdate();
			ResultSet rs = psmt.executeQuery();
			rs.next();
			rowno = rs.getString(1);
		} catch (SQLException e) {

			log.error("Error in inserting row in asp_api_dashboard table" + e.getMessage() + " " + e.getStackTrace()
					+ " " + e.getStackTrace());
		} finally {
			try {
				psmt.close();
				conn.close();
				return rowno;
			} catch (SQLException e) {
				log.error("Error in inserting row in asp_api_dashboard table" + e.getMessage() + " " + e.getStackTrace()
						+ " " + e.getStackTrace());
			}

		}
		return rowno;
	}

	public void updateApiDashboard(String rowno, String response) {
		final String updateTransaction = "UPDATE public.asp_api_dashboard SET response = ?, resp_timestamp = ? where id=?";

		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(updateTransaction)) {
			stmt.setString(1, response);
			stmt.setTimestamp(2, getCurrentTimestamp());
			stmt.setString(3, rowno);
			stmt.executeUpdate();

			stmt.close();
			con.close();
			log.info("Table asp_api_dashboard updated Sucessfully with row no: " + rowno);
		} catch (Exception e) {
			log.error("Error in updating asp_api_dashboard " + e.getMessage() + " " + e.getStackTrace());
		}
	}

	public String getCountryName(String countryID) {
		Connection conn = null;
		PreparedStatement psmt = null;
		String countryName = "";
		String selectQuery = "SELECT cg_name FROM public.lrit_contract_govt_mst where cg_lritid =? limit 1";
		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(selectQuery);
			psmt.setString(1, countryID);

			log.info("query=" + psmt.toString());
			ResultSet rs = psmt.executeQuery();
			rs.next();
			countryName = rs.getString(1);
			// log.info("status is "+strr);
		} catch (SQLException e) {

			log.error("Error in fetching country name" + e.getMessage() + " " + e.getStackTrace() + " "
					+ e.getStackTrace());
			return countryName;
		} finally {
			try {
				psmt.close();

				conn.close();
				return countryName;
			} catch (SQLException e) {
				log.error("Error in fetching country name" + e.getMessage() + " " + e.getStackTrace() + " "
						+ e.getStackTrace());
			}

		}
		return countryName;

	}

	public List<String> getDummyImoList() {

		log.info("fetching imo numbers from database");
		ArrayList<String> imo_list = new ArrayList<>();
		String GetImo_numQuery = "SELECT distinct(imo_no) FROM portal_vessel_details limit 10";
		try (

				Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(GetImo_numQuery)) {

			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next())
					imo_list.add(rs.getString(1));

			}
		} catch (SQLException e) {

			log.error("error in fetch imo number" + e.getMessage() + e.getStackTrace());

		}

		log.info("[ " + imo_list + "] Imo_Numbers list extracted from database");
		return imo_list;

	}

	// done
	public boolean imoExists(String imo_no) {
		log.info("checking IMO No.::" + "[" + imo_no + "]");
		final String selectTransaction = "Select * From portal_vessel_details where imo_no=?";
		boolean flag = false;

		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(selectTransaction)) {
			stmt.setString(1, imo_no);
			ResultSet rs = stmt.executeQuery();
			return rs.next();

		} catch (Exception e) {
			log.error("Error in checking IMO :" + e.getMessage() + " " + e.getStackTrace());
			return false;
		}
	}

	public ArrayList<Requestlist> FetchActiveRequests() {
		ArrayList<Requestlist> reqlist = new ArrayList<Requestlist>();

		Connection conn = null;
		PreparedStatement psmt = null;

		try {
			conn = dataSource.getConnection();
			psmt = conn.prepareStatement(FetchSetRequests);
			//psmt.setString(1, imoNumber);
			ResultSet rs = psmt.executeQuery();

			while (rs.next()) {
				Requestlist list = new Requestlist();

				list.setMessage_id(rs.getString(1));
				list.setReferencecspId(rs.getString(2));
				list.setSet_pollcommand(rs.getString(3));
				list.setStart_pollcommand(rs.getString(4));
				list.setImo_number(rs.getString(5));
				list.setDnid_no(rs.getInt(6));
				list.setMember_no(rs.getInt(7));
				list.setSet_sending_timestamp(rs.getTimestamp(8));
				list.setStart_sending_timestamp(rs.getTimestamp(9));

				list.setLritTimestamp(rs.getTimestamp(10));
				list.setSet_is_active(rs.getBoolean(11));
				list.setStart_is_active(rs.getBoolean(12));

				reqlist.add(list);

			}
			return reqlist;

		} catch (SQLException e) {
			log.error("Error in fetching active SET request details " + e.getMessage() + " " + e.getStackTrace());
			return null;
		} finally {
			try {
				psmt.close();

				conn.close();
			} catch (SQLException e) {
				log.error("Error in fetching country name" + e.getMessage() + " " + e.getStackTrace() + " "
						+ e.getStackTrace());
			}

		}

	}

	public ArrayList<Requestlist> FetchStartActiveRequests() {
	  ArrayList<Requestlist > reqlist = new ArrayList<Requestlist >();
	  
	  Connection conn = null; PreparedStatement psmt = null;
	  
	  try { conn = dataSource.getConnection(); 
	  psmt = conn.prepareStatement(FetchStartRequest);
	  //psmt.setString(1, imoNumber); 
	  ResultSet rs = psmt.executeQuery();
	  
	  while (rs.next()) { Requestlist list=new Requestlist();
	  
	  list.setMessage_id(rs.getString(1)); list.setReferencecspId(rs.getString(2));
	  list.setSet_pollcommand(rs.getString(3));
	  list.setStart_pollcommand(rs.getString(4));
	  list.setImo_number(rs.getString(5)); list.setDnid_no(rs.getInt(6));
	  list.setMember_no(rs.getInt(7));
	  list.setStart_sending_timestamp(rs.getTimestamp(8));
	  list.setSet_sending_timestamp(rs.getTimestamp(9));
	  list.setLritTimestamp(rs.getTimestamp(10));
	  list.setSet_is_active(rs.getBoolean(11));
	  list.setStart_is_active(rs.getBoolean(12));
	  
	  
	  reqlist.add(list); 
	  } return reqlist;
	  
	  
	  } catch (SQLException e) {
	  log.error("Error in fetching active START request details " + e.getMessage()
	  + " " + e.getStackTrace()); return null; } 
	  finally { try { psmt.close();
	  
	  conn.close(); } catch (SQLException e) {
	  log.error("Error in fetching country name" + e.getMessage() + " " +
	  e.getStackTrace() + " " + e.getStackTrace()); }
	  
	  }
	  
	  
	  }

	public boolean updateSetPollDB(String message_id) {

		final String updateTransaction = "UPDATE public.asp_poll_request SET set_is_active = ? where message_id=?";

		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(updateTransaction)) {

			stmt.setBoolean(1, false);
			stmt.setString(2, message_id);

			stmt.executeUpdate();
			stmt.close();
			con.close();
			return true;
		} catch (Exception e) {
			log.error("Error in updating asp_setpoll DB " + e.getMessage() + " " + e.getStackTrace());
			return false;
		}
	}

	public boolean updateStartPollDB(String message_id) {

		final String updateTransaction = "UPDATE public.asp_poll_request SET start_is_active = ? where message_id=?";

		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(updateTransaction)) {

			stmt.setBoolean(1, false);
			stmt.setString(2, message_id);

			stmt.executeUpdate();
			stmt.close();
			con.close();
			return true;
		} catch (Exception e) {
			log.error("Error in updating asp_startpoll DB " + e.getMessage() + " " + e.getStackTrace());
			return false;
		}
	}

	public boolean insertPollCommand(String messageID, String referenceCspId, String setPollCommand,
			String startPollCommand, String imo_no, String dnid_no, int member_no, Timestamp set_sending_timestamp,
			Timestamp start_sending_timestamp, Timestamp lrit_timestamp, boolean set_is_active,
			boolean start_is_active) {
		// TODO Auto-generated method stub
		final String InsertTransaction = "INSERT INTO public.asp_poll_request(\n"
				+ "	message_id, referenceCspId, set_pollcommand, start_pollcommand,  imo_no, dnid_no, member_no, set_sending_timestamp,start_sending_timestamp, lrit_timestamp, set_is_active,start_is_active)\n"
				+ "	VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?)";

		try (Connection con = getDataSource().getConnection();
				PreparedStatement stmt = con.prepareStatement(InsertTransaction)) {
			stmt.setString(1, messageID);
			stmt.setString(2, referenceCspId);
			stmt.setString(3, startPollCommand);
			stmt.setString(4, setPollCommand);
			stmt.setString(5, imo_no);
			stmt.setInt(6, Integer.parseInt(dnid_no));
			stmt.setInt(7, (member_no));
			stmt.setObject(8, set_sending_timestamp);
			stmt.setObject(9, start_sending_timestamp);
			stmt.setObject(10, lrit_timestamp);
			stmt.setObject(11, set_is_active);
			stmt.setObject(12, start_is_active);

			stmt.executeUpdate();
			// log.info("[ " + message_id + "] Inserted in ASP Set Poll table ");

			stmt.close();
			con.close();
			return true;
		} catch (Exception e) {
			log.error("Error in inserting asp_setPoll table " + e.getMessage() + " " + e.getStackTrace());
			return false;
		}
	}
}