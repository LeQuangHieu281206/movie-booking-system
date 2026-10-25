package com.ttcsn.payment.controller;

import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API thanh toán (mô phỏng).
 * userId lấy từ X-User-Id do gateway gắn; amount lấy từ booking ở server.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    public record PaymentRequest(
            Long bookingId,
            Boolean simulateFailure
    ) {}

    @PostMapping("/createPayment")
    public ResponseEntity<PaymentCompletedEvent> createPayment(
            @RequestBody PaymentRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        PaymentCompletedEvent event = paymentService.processPayment(
                request.bookingId(),
                userId,
                Boolean.TRUE.equals(request.simulateFailure()));
        return ResponseEntity.status(201).body(event);
    }
}
