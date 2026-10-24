package com.ttcsn.payment.controller;

import com.ttcsn.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST Controller quản lý các API liên quan đến thao tác thanh toán từ phía Client.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    public record PaymentRequest(
            Long bookingId,
            Long userId,
            BigDecimal amount
    ) {}

    public record PaymentResponse(
            String qrUrl,
            String message
    ) {}

    /**
     * Khởi tạo giao dịch thanh toán và trả về đường dẫn ảnh mã QR.
     * Tích hợp theo cơ chế VietQR thông qua SePay API.
     */
    @PostMapping("/createPayment")
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody PaymentRequest request) {
        String qrUrl = paymentService.generateSePayQrUrl(
                request.bookingId(),
                request.userId(),
                request.amount()
        );

        return ResponseEntity.status(201).body(new PaymentResponse(qrUrl, "Vui lòng quét mã QR để hoàn tất thanh toán."));
    }
}