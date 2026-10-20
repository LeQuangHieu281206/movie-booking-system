package com.ttcsn.movie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Movie & Genre & Showtime (Seat Inventory).
 * Port 8081. Gọi sang venue-service (Room/Seat) qua OpenFeign.
 */
@SpringBootApplication(scanBasePackages = "com.ttcsn")
@EnableFeignClients(basePackages = "com.ttcsn.movie.client")
public class MovieServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MovieServiceApplication.class, args);
    }
}
