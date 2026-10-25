package com.ttcsn.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * User & Auth (JWT stateless HS256 — jjwt 0.12.6).
 * Port 8084. Không phụ thuộc service nào.
 */
@SpringBootApplication(scanBasePackages = "com.ttcsn")
@ConfigurationPropertiesScan("com.ttcsn.user.config")
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
