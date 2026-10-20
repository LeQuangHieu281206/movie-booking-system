package com.ttcsn.common.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * SOA Step 3 — Event phát ra khi payment-service xử lý thanh toán xong.
 * routing-key: "payment.completed" (Topics.PAYMENT_COMPLETED)
 *
 * booking-service  : cập nhật Booking.status -> PAID (SUCCESS) / CANCELLED (FAILED)
 * notification-service: log "Đã gửi Email vé cho khách hàng"
 *
 * Jackson2JsonMessageConverter — OffsetDateTime cần ObjectMapper có JavaTimeModule
 * (inject ObjectMapper của Boot vào converter).
 */
public record PaymentCompletedEvent(
        UUID paymentId,
        Long bookingId,
        Long userId,
        BigDecimal amount,
        OffsetDateTime paymentTime,
        String status          // SUCCESS | FAILED
) {
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED  = "FAILED";
}
