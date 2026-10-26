package com.ttcsn.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Booking & BookingDetail (Saga orchestrator sau này).
 * Port 8082. Gọi user-service + movie-service qua OpenFeign.
 */
@SpringBootApplication(scanBasePackages = "com.ttcsn")
@EnableFeignClients(basePackages = "com.ttcsn.booking.client")
@EnableScheduling
public class BookingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
