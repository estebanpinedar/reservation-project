package co.edu.uniajc.rsu_auth_service;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import co.edu.uniajc.rsu_auth_service.security.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class RsuAuthServiceApplication {
    public static void main(String[] args) {
        // Keep java.sql.Time conversion aligned with Hibernate's UTC JDBC timezone.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(RsuAuthServiceApplication.class, args);
    }
}
