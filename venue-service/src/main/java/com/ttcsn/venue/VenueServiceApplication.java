package com.ttcsn.venue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 *
 * @author quanghieu
 */
@SpringBootApplication(scanBasePackages = "com.ttcsn")
public class VenueServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VenueServiceApplication.class, args);
    }
}