package com.ttcsn.payment.service;

import com.ttcsn.common.dto.BookingPaymentView;
import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.common.event.Topics;
import com.ttcsn.common.exception.ConflictException;
import com.ttcsn.common.exception.NotFoundException;
import com.ttcsn.payment.client.BookingClient;
import com.ttcsn.payment.config.RabbitMQConfig;
import com.ttcsn.payment.dto.request.SePayWebhookDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ thanh toán tích hợp cổng SePay (mô hình SOA).
 * Chịu trách nhiệm tạo mã QR thanh toán và xử lý Webhook tự động khi có biến động số dư.
 *
 * <p>An toàn dữ liệu: bookingId duy nhất client được phép gửi; userId lấy từ X-User-Id
 * (gateway gắn sau khi verify JWT) và amount luôn đọc từ booking ở server qua
 * {@link BookingClient} — client không thể tự định giá giao dịch hay thanh toán
 * đơn của người khác (404 khi không sở hữu, không lộ booking tồn tại hay không).
 *
 * @author quanghieu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingClient bookingClient;
    private final RabbitTemplate rabbitTemplate;

    // Thông tin tài khoản ngân hàng thụ hưởng (Có thể cấu hình hóa vào file application.yml)
    private final String BANK_NAME = "MBBank";
    private final String ACCOUNT_NUMBER = "1234567890";

    // Tiền tố quy ước cho nội dung chuyển khoản để định danh mã đơn hàng (VD: "DH")
    private final String BOOKING_PREFIX = "DH";

    /**
     * Xác thực booking rồi sinh URL hình ảnh mã QR theo chuẩn VietQR.
     * Client (Frontend) sử dụng đường dẫn này để hiển thị mã QR thanh toán cho người dùng.
     */
    public String createSePayPayment(Long bookingId, Long userId) {
        if (bookingId == null) {
            throw new IllegalArgumentException("bookingId không được để trống");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Thiếu X-User-Id");
        }

        BookingPaymentView booking = loadOwnedPendingBooking(bookingId, userId);
        BigDecimal amount = booking.totalAmount();

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
     * Đọc booking qua internal API và kiểm tra: tồn tại, đúng chủ sở hữu, đang PENDING,
     * có số tiền hợp lệ. Booking không tồn tại hoặc không thuộc user → 404 (không lộ
     * booking của người khác có tồn tại hay không); sai trạng thái → 409.
     */
    private BookingPaymentView loadOwnedPendingBooking(Long bookingId, Long userId) {
        BookingPaymentView booking;
        try {
            booking = bookingClient.getBooking(bookingId);
        } catch (FeignException.NotFound ex) {
            throw new NotFoundException("Không tìm thấy đơn đặt vé với bookingId = " + bookingId);
        }
        if (booking == null || !Objects.equals(booking.userId(), userId)) {
            throw new NotFoundException("Không tìm thấy đơn đặt vé với bookingId = " + bookingId);
        }
        if (!"PENDING".equals(booking.status())) {
            throw new ConflictException("Đơn đặt vé không ở trạng thái chờ thanh toán");
        }
        if (booking.totalAmount() == null || booking.totalAmount().signum() <= 0) {
            throw new ConflictException("Đơn đặt vé chưa có số tiền hợp lệ");
        }
        return booking;
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
