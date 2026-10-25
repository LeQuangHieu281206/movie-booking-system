package com.ttcsn.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Payment Service — Phân rã theo mô hình Microservices / SOA.
 * Chịu trách nhiệm xử lý cổng thanh toán tích hợp (VietQR / SePay Webhook) 
 * và phát sự kiện độc lập qua Message Broker (RabbitMQ).
 * 
 * <p><b>Cấu hình kỹ thuật cốt lõi:</b>
 * <ul>
 *   <li>Chạy trên cổng cố định: {@code 8086}.</li>
 *   <li>Loại trừ tự động cấu hình Database ({@code DataSourceAutoConfiguration}, 
 *       {@code HibernateJpaAutoConfiguration}) do module dùng chung {@code common} 
 *       có tích hợp sẵn JPA trên classpath, giúp tối ưu hóa tài nguyên vì service này hoàn toàn stateless.</li>
 * </ul>
 * 
 * @author Nhóm phát triển (Module Payment Service)
 */
@Slf4j
@SpringBootApplication(
        scanBasePackages = "com.ttcsn",
        exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@EnableFeignClients(basePackages = "com.ttcsn.payment.client")
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
        log.info("==================================================");
        log.info("[payment-service] Đã khởi động thành công trên cổng 8086!");
        log.info("[payment-service] Sẵn sàng lắng nghe Webhook và RabbitMQ.");
        log.info("==================================================");
    }
}
