package in.gov.lrit.asp.config;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceConfig {
	@Primary
	@Bean(name = "servicePostgresDS")
	@ConfigurationProperties(prefix = "spring.datasource")
	public DataSource getLRITDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "serviceddpdbds")
	@ConfigurationProperties(prefix = "spring.ddp-datasource")
	public DataSource getDDPDataSource() {
		return DataSourceBuilder.create().build();
	}

}
