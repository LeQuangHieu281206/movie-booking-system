package com.ttcsn.payment.service;

import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.common.event.Topics;
import com.ttcsn.payment.config.RabbitMQConfig;
import com.ttcsn.payment.dto.request.SePayWebhookDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ thanh toán tích hợp cổng SePay (mô hình SOA).
 * Chịu trách nhiệm tạo mã QR thanh toán và xử lý Webhook tự động khi có biến động số dư.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RabbitTemplate rabbitTemplate;

    // Thông tin tài khoản ngân hàng thụ hưởng (Có thể cấu hình hóa vào file application.yml)
    private final String BANK_NAME = "MBBank"; 
    private final String ACCOUNT_NUMBER = "1234567890";
    
    // Tiền tố quy ước cho nội dung chuyển khoản để định danh mã đơn hàng (VD: "DH")
    private final String BOOKING_PREFIX = "DH"; 

    /**
     * Sinh URL hình ảnh mã QR theo chuẩn VietQR.
     * Client (Frontend) sử dụng đường dẫn này để hiển thị mã QR thanh toán cho người dùng.
     */
    public String generateSePayQrUrl(Long bookingId, Long userId, BigDecimal amount) {
        String description = BOOKING_PREFIX + bookingId;
        String qrUrl = String.format("https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%s&des=%s",
                ACCOUNT_NUMBER, BANK_NAME, amount.toPlainString(), description);
                
        log.info("[payment-service] Đã khởi tạo URL mã QR cho bookingId={}. Link: {}", bookingId, qrUrl);
        return qrUrl;
    }

    /**
     * Xử lý payload Webhook do hệ thống SePay gọi tới khi có giao dịch.
     * Nếu giao dịch hợp lệ, hệ thống sẽ publish sự kiện PaymentCompletedEvent qua RabbitMQ.
     */
    public void processSePayWebhook(SePayWebhookDto request) {
        // Nghiệp vụ chỉ ghi nhận các giao dịch nhận tiền ("in")
        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            return;
        }

        String content = request.getContent();
        if (content == null) return;
        
        // Trích xuất mã giao dịch (bookingId) từ nội dung chuyển khoản
        Long bookingId = extractBookingIdFromContent(content.toUpperCase(), BOOKING_PREFIX);

        if (bookingId != null) {
            // Khởi tạo Event để thông báo thanh toán thành công tới các dịch vụ khác.
            // Lưu ý: Webhook không cung cấp userId, Consumer cần tự truy vấn thông tin này thông qua bookingId.
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    UUID.randomUUID(),
                    bookingId,
                    null, 
                    request.getTransferAmount(),
                    OffsetDateTime.now(),
                    PaymentCompletedEvent.STATUS_SUCCESS
            );

            // Publish sự kiện lên RabbitMQ Exchange
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, Topics.PAYMENT_COMPLETED, event);
            log.info("[payment-service] Xử lý Webhook SePay thành công. Đã publish event cho bookingId={}, amount={}",
                    bookingId, request.getTransferAmount());
        } else {
            log.warn("[payment-service] Nhận được giao dịch nhưng không tìm thấy mã đối soát (Prefix: {}) trong nội dung: {}", BOOKING_PREFIX, content);
        }
    }

    /**
     * Hàm tiện ích hỗ trợ trích xuất dãy số định danh từ chuỗi văn bản dựa vào tiền tố quy ước.
     */
    private Long extractBookingIdFromContent(String content, String prefix) {
        try {
            if (content.contains(prefix)) {
                int startIndex = content.indexOf(prefix) + prefix.length();
                StringBuilder idStr = new StringBuilder();
                for (int i = startIndex; i < content.length(); i++) {
                    char c = content.charAt(i);
                    if (Character.isDigit(c)) {
                        idStr.append(c);
                    } else {
                        break;
                    }
                }
                if (!idStr.isEmpty()) {
                    return Long.parseLong(idStr.toString());
                }
            }
        } catch (Exception e) {
            log.error("[payment-service] Lỗi trích xuất Booking ID từ nội dung Webhook: {}", content, e);
        }
        return null;
    }
}