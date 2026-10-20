package com.ttcsn.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Notification Service — gửi Email/SMS vé cho khách.
 * Port 8087. Lắng nghe "payment.completed" từ RabbitMQ (payment-service phát).
 * (common mang JPA trên classpath -> loại trừ DataSource/JPA auto-config: service không cần DB)
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
