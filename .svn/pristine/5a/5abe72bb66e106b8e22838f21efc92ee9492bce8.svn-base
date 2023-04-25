package in.gov.lrit.asp.config;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CXFConfig {
	
	@Bean
    public ServletRegistrationBean<CXFServlet> dispServlet() {
    	final ServletRegistrationBean<CXFServlet> servletRegistrationBean = new ServletRegistrationBean<CXFServlet>(new CXFServlet(), "/services/*");
        servletRegistrationBean.setLoadOnStartup(1);
        return servletRegistrationBean;
    }

    @Bean(name = Bus.DEFAULT_BUS_ID)
    public SpringBus springBus() {
        SpringBus springBus = new SpringBus();
        return springBus;
    }
    
    
}