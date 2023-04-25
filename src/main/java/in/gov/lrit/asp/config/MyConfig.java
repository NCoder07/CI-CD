package in.gov.lrit.asp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Configuration
public class MyConfig {

	@Value("${ASP}")
	public String ASP;
	
	@Value("${CSP}")
	public String CSP;
	
	@Value("${FROM}")
	public String FROM;
	
	@Value("${outgoingIp}")
	public String outgoingIp;
	
	@Value("${incomingIp}")
	public String incomingIp;
	
	@Value("${username}")
	private String username;
	
	
	@Value("${password}")
	public String password;
	
}
