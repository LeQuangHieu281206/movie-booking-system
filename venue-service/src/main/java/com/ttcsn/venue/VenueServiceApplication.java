package com.ttcsn.venue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Cinema, Room, Seat (venue context).
 * Port 8083. Expose /internal/rooms/** cho movie-service.
 */
@SpringBootApplication(scanBasePackages = "com.ttcsn")
public class VenueServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VenueServiceApplication.class, args);
    }
}
