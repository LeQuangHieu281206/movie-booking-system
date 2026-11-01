package com.ttcsn.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Payment Service — Phân rã theo mô hình Microservices / SOA.
 * Chịu trách nhiệm xử lý cổng thanh toán tích hợp (VietQR / SePay Webhook) 
 * và phát sự kiện độc lập qua Message Broker (RabbitMQ).
 * 
 * <p><b>Cấu hình kỹ thuật cốt lõi:</b>
 * <ul>
 *   <li>Chạy trên cổng cố định: {@code 8086}.</li>
 *   <li>Dùng chung Supabase PostgreSQL để lưu lịch sử giao dịch trong bảng {@code payments}.</li>
 * </ul>
 * 
 * @author Nhóm phát triển (Module Payment Service)
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.ttcsn")
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
