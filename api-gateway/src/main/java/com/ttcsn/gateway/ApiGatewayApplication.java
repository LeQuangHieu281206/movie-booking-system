package com.ttcsn.gateway;

import com.ttcsn.gateway.config.GatewaySecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * API Gateway — điểm vào duy nhất của frontend (:8080).
 * Route /api/** tới user-service, movie-service, venue-service, booking-service.
 */
@SpringBootApplication
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
