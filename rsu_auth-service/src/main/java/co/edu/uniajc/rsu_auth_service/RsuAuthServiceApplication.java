package co.edu.uniajc.rsu_auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import co.edu.uniajc.rsu_auth_service.security.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class RsuAuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RsuAuthServiceApplication.class, args);
    }
}
