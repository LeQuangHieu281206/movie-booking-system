package com.ttcsn.payment.controller;

import com.ttcsn.payment.dto.request.SePayWebhookDto;
import com.ttcsn.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller đóng vai trò Webhook Listener.
 * Tiếp nhận tín hiệu callback tự động từ hệ thống SePay khi có giao dịch biến động số dư.
 *
 * <p>Xác thực nguồn gọi: nếu cấu hình {@code app.sepay.webhook-api-key} (biến môi trường
 * SEPAY_WEBHOOK_API_KEY) thì request phải mang đúng key trong header Authorization
 * (Bearer) hoặc X-Secret-Key; sai → 401. Khi chưa cấu hình (dev) webhook vẫn mở
 * nhưng log cảnh báo — production bắt buộc đặt key.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SepayController {

    private final PaymentService paymentService;

    @Value("${app.sepay.webhook-api-key:}")
    private String webhookApiKey;

    /**
     * Endpoint tiếp nhận POST request từ máy chủ SePay.
     * Yêu cầu expose public trong môi trường phát triển (sử dụng Ngrok hoặc localtunnel).
     */
    @PostMapping("/sepayWebhook")
    public ResponseEntity<String> sepayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Secret-Key", required = false) String secretKey,
            @RequestBody SePayWebhookDto request) {

        if (!isAuthorizedCaller(authorization, secretKey)) {
            log.warn("[payment-service] Webhook SePay bị từ chối: API key không hợp lệ");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("API key không hợp lệ");
        }

        try {
            paymentService.processSePayWebhook(request);
            return ResponseEntity.ok("Xử lý Webhook SePay thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xử lý hệ thống Webhook: " + e.getMessage());
        }
    }

    private boolean isAuthorizedCaller(String authorization, String secretKey) {
        if (!StringUtils.hasText(webhookApiKey)) {
            log.warn("[payment-service] CHƯA cấu hình app.sepay.webhook-api-key — webhook đang mở cho mọi nguồn gọi (chỉ dùng khi dev)");
            return true;
        }
        if (StringUtils.hasText(secretKey) && webhookApiKey.equals(secretKey)) {
            return true;
        }
        return StringUtils.hasText(authorization)
                && authorization.startsWith("Bearer ")
                && webhookApiKey.equals(authorization.substring("Bearer ".length()));
    }
}
