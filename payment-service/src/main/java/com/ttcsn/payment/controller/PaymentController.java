package com.ttcsn.payment.controller;

import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API thanh toán (mô phỏng) — sau này booking-service gọi đến khi booking PENDING,
 * hoặc frontend redirect sang payment-gateway rồi callback vào đây.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    public record PaymentRequest(
            Long bookingId,
            Long userId,
            java.math.BigDecimal amount,
            Boolean simulateFailure
    ) {}

    @PostMapping("/createPayment")
    public ResponseEntity<PaymentCompletedEvent> createPayment(@RequestBody PaymentRequest request) {
        PaymentCompletedEvent event = paymentService.processPayment(
                request.bookingId(),
                request.userId(),
                request.amount(),
                Boolean.TRUE.equals(request.simulateFailure()));
        return ResponseEntity.status(201).body(event);
    }
}
