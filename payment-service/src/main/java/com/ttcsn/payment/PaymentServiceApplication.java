package com.ttcsn.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Payment Service — cổng thanh toán (mô phỏng / sau này nối VNPay/MoMo/Stripe).
 * Port 8086. Publish PaymentCompletedEvent lên RabbitMQ khi thanh toán xong.
 * (common mang JPA trên classpath -> loại trừ DataSource/JPA auto-config: service không cần DB)
 */
@SpringBootApplication(
        scanBasePackages = "com.ttcsn",
        exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@EnableFeignClients(basePackages = "com.ttcsn.payment.client")
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

