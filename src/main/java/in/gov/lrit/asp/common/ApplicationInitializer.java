package in.gov.lrit.asp.common;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import in.gov.lrit.asp.config.MyConfig;

/**
 * This class is used for initializing application through config_para table.
 * Key and value are set in system variables.
 * @author Lrit team
 *
 */
public class ApplicationInitializer implements ApplicationContextAware{
	
		@Autowired
		MyConfig config;
		
		//private Logger log =Logger.getLogger(ApplicationInitializer.class); 
		Logger log =(Logger) LoggerFactory.getLogger(ApplicationInitializer.class);
		private ApplicationContext ac=null;
		DBUpdation dbProcess;

		public DBUpdation getDbProcess() {
			return dbProcess;
		}

		public void setDbProcess(DBUpdation dbProcess) {
			this.dbProcess = dbProcess;
		}
	
		
		

		public void initialise(){
			Connection con=null;
			log.info("The LRIT Application is being initialised.");
			
			log.info("********************* Printing Global properties ********************");
			log.info("ASP: "+config.getASP());
			log.info("CSP: "+config.getCSP());
			log.info("FROM: "+config.getFROM());
			log.info("Incoming IP: "+config.getIncomingIp());
			log.info("Outgoing IP: "+config.getOutgoingIp());
			log.info("Username: "+config.getUsername());
			log.info("Password for Email: "+config.getPassword());
			log.info("********************* End of Printing Global Properties ********************");
			
			
			try{
			Properties p = System.getProperties();
			
					 con = dbProcess.getDataSource().getConnection();
					PreparedStatement stmt = con.prepareStatement(InitailizeSystem);
					ResultSet rs = stmt.executeQuery();
					while(rs.next()){
						if (rs.getString(1).equals("javax.net.ssl.keyStore") || rs.getString(1).equals("javax.net.ssl.trustStore") )
						{
							log.info("Not setting properties for certificates" + rs.getString(1));
						}
						else
						{
							p.setProperty(rs.getString(1),rs.getString(2));
							log.info("================="+rs.getString(1) +" =====>  "+ rs.getString(2));
							
						}
					//p.setProperty(rs.getString(1),rs.getString(2));
					//log.info(rs.getString(1) +" =====>  "+ rs.getString(2));
					}
			}
			catch(Exception e)
			{
				
				log.error(" in  initialise Exception "+e.getMessage()+" *** "+e.getCause()+" *** "+e.getStackTrace().toString());
			
				
			}
			finally {
				if (con != null) {
					try {
						con.close();
					} catch (Exception e) {
						log.error("finally "+e.getMessage(),e);
					}
				}
			}
			
	}
		@Override
		public void setApplicationContext(ApplicationContext ac)
				throws BeansException {
			this.ac=ac;
			
		}
		
		
		static final String InitailizeSystem="select * from config_para";
}



