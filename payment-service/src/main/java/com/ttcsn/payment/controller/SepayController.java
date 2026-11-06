package com.ttcsn.payment.controller;

import com.ttcsn.payment.config.SepayProperties;
import com.ttcsn.payment.dto.request.SePayWebhookDto;
import com.ttcsn.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
public class SepayController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final PaymentService paymentService;
    private final SepayProperties sepayProperties;

    /**
     * Endpoint tiếp nhận POST request từ máy chủ SePay.
     * Lỗi xử lý để ApiExceptionHandler trả JSON lỗi chuẩn, không lộ chi tiết nội bộ.
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

        paymentService.processSePayWebhook(request);
        return ResponseEntity.ok("Xử lý Webhook SePay thành công");
    }

    private boolean isAuthorizedCaller(String authorization, String secretKey) {
        String webhookApiKey = sepayProperties.webhookApiKey();
        if (!StringUtils.hasText(webhookApiKey)) {
            log.warn("[payment-service] CHƯA cấu hình app.sepay.webhook-api-key — webhook đang mở cho mọi nguồn gọi (chỉ dùng khi dev)");
            return true;
        }
        if (StringUtils.hasText(secretKey) && webhookApiKey.equals(secretKey)) {
            return true;
        }
        return StringUtils.hasText(authorization)
                && authorization.startsWith(BEARER_PREFIX)
                && webhookApiKey.equals(authorization.substring(BEARER_PREFIX.length()));
    }
}
