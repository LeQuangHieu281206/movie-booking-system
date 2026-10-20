package com.ttcsn.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * User & Auth (sau này thêm JWT).
 * Port 8084. Không phụ thuộc service nào.
 */
@SpringBootApplication(scanBasePackages = "com.ttcsn")
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
