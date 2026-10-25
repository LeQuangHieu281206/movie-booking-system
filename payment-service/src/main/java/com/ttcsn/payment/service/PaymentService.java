package com.ttcsn.payment.service;

import com.ttcsn.common.dto.BookingPaymentView;
import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.common.event.Topics;
import com.ttcsn.common.exception.ConflictException;
import com.ttcsn.common.exception.NotFoundException;
import com.ttcsn.payment.client.BookingClient;
import com.ttcsn.payment.config.RabbitMQConfig;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SOA Step 3 — Service thanh toán (mô phỏng).
 * Xác thực booking (tồn tại, đúng user, PENDING, amount server-side) rồi mới publish.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingClient bookingClient;
    private final RabbitTemplate rabbitTemplate;

    /** shortcut: idempotency trong bộ nhớ, mất khi restart hoặc chạy nhiều instance; nâng cấp DB/Redis khi scale. */
    private final ConcurrentHashMap<Long, PaymentCompletedEvent> processedPayments = new ConcurrentHashMap<>();

    public PaymentCompletedEvent processPayment(Long bookingId, Long userId, boolean simulateFailure) {
        if (bookingId == null) {
            throw new IllegalArgumentException("bookingId không được để trống");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Thiếu X-User-Id");
        }

        PaymentCompletedEvent existing = processedPayments.get(bookingId);
        if (existing != null) {
            return ownedBy(existing, userId);
        }

        PaymentCompletedEvent event = processedPayments.computeIfAbsent(bookingId,
                id -> createPayment(id, userId, simulateFailure));
        // Request đồng thời của user khác có thể nhận event của người đầu tiên; không được trả event đó.
        return ownedBy(event, userId);
    }

    private PaymentCompletedEvent createPayment(Long bookingId, Long userId, boolean simulateFailure) {
        BookingPaymentView booking;
        try {
            booking = bookingClient.getBooking(bookingId);
        } catch (FeignException.NotFound ex) {
            throw new NotFoundException("Không tìm thấy đơn đặt vé với bookingId = " + bookingId);
        }
        if (booking == null || !Objects.equals(booking.userId(), userId)) {
            // 404: không lộ booking của người khác có tồn tại hay không.
            throw new NotFoundException("Không tìm thấy đơn đặt vé với bookingId = " + bookingId);
        }
        if (!"PENDING".equals(booking.status())) {
            throw new ConflictException("Đơn đặt vé không ở trạng thái chờ thanh toán");
        }
        BigDecimal amount = booking.totalAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new ConflictException("Đơn đặt vé chưa có số tiền hợp lệ");
        }

        boolean success = !simulateFailure;
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                booking.bookingId(),
                booking.userId(),
                amount,
                OffsetDateTime.now(),
                success ? PaymentCompletedEvent.STATUS_SUCCESS : PaymentCompletedEvent.STATUS_FAILED
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, Topics.PAYMENT_COMPLETED, event);
        log.info("[payment-service] Đã publish PaymentCompletedEvent: bookingId={}, status={}",
                event.bookingId(), event.status());
        return event;
    }

    private static PaymentCompletedEvent ownedBy(PaymentCompletedEvent event, Long userId) {
        if (!Objects.equals(event.userId(), userId)) {
            throw new NotFoundException("Không tìm thấy đơn đặt vé với bookingId = " + event.bookingId());
        }
        return event;
    }
}
