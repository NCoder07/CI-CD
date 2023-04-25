package in.gov.lrit.asp;



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@ComponentScan(basePackages = "in.gov.lrit.asp")
@ImportResource({"classpath:spring/spring-cxf-config.xml", "classpath:spring/LRITBeanConfig.xml","classpath*:/route/aspsimulator.xml"})
@Slf4j
@Import({ in.gov.lrit.asp.config.DataSourceConfig.class})
public class ASP_Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(ASP_Application.class, args);
		//log.info("ASP Application Main Method Started");
		
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(ASP_Application.class);
	}

}
