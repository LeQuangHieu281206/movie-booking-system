package com.ttcsn.payment.controller;

import com.ttcsn.payment.dto.request.SePayWebhookDto;
import com.ttcsn.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller đóng vai trò Webhook Listener.
 * Tiếp nhận tín hiệu callback tự động từ hệ thống SePay khi có giao dịch biến động số dư.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SepayController {

    private final PaymentService paymentService;

    /**
     * Endpoint tiếp nhận POST request từ máy chủ SePay.
     * Yêu cầu expose public trong môi trường phát triển (sử dụng Ngrok hoặc localtunnel).
     */
    @PostMapping("/sepayWebhook")
    public ResponseEntity<String> sepayWebhook(@RequestBody SePayWebhookDto request) {
        try {
            paymentService.processSePayWebhook(request);
            return ResponseEntity.ok("Xử lý Webhook SePay thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xử lý hệ thống Webhook: " + e.getMessage());
        }
    }
}