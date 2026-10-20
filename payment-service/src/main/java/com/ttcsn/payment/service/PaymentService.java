package com.ttcsn.payment.service;

import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.common.event.Topics;
import com.ttcsn.payment.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * SOA Step 3 — Service thanh toán (mô phỏng).
 * Sau khi "thanh toán thành công" -> publish PaymentCompletedEvent lên exchange
 * với routing-key "payment.completed"; booking-service & notification-service cùng nghe.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RabbitTemplate rabbitTemplate;

    public PaymentCompletedEvent processPayment(Long bookingId, Long userId, BigDecimal amount, boolean simulateFailure) {
        boolean success = !simulateFailure;

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                bookingId,
                userId,
                amount,
                OffsetDateTime.now(),
                success ? PaymentCompletedEvent.STATUS_SUCCESS : PaymentCompletedEvent.STATUS_FAILED
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, Topics.PAYMENT_COMPLETED, event);
        log.info("[payment-service] Đã publish PaymentCompletedEvent: bookingId={}, status={}",
                event.bookingId(), event.status());

        return event;
    }
}
