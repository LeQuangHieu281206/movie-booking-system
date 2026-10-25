package com.ttcsn.payment.controller;

import com.ttcsn.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller quản lý các API thanh toán từ phía Client.
 * userId lấy từ X-User-Id do gateway gắn sau khi verify JWT (client không tự gửi được);
 * amount lấy từ booking ở server qua BookingClient — client chỉ cần gửi bookingId.
 *
 * @author quanghieu
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    public record PaymentRequest(
            Long bookingId
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
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody PaymentRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        String qrUrl = paymentService.createSePayPayment(request.bookingId(), userId);
        return ResponseEntity.status(201)
                .body(new PaymentResponse(qrUrl, "Vui lòng quét mã QR để hoàn tất thanh toán."));
    }
}
