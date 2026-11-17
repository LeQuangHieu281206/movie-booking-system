package com.ttcsn.payment.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ttcsn.payment.dto.response.PaymentHistoryResponse;
import com.ttcsn.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PaymentResponse(
            String qrUrl,
            String message,
            Long bookingId,
            String referenceCode,
            String bankName,
            String accountNumber,
            String accountName
    ) {
        public PaymentResponse(String qrUrl, String message) {
            this(qrUrl, message, null, null, null, null, null);
        }
    }

    /**
     * Khởi tạo giao dịch thanh toán và trả về đường dẫn ảnh mã QR kèm thông tin chuyển khoản.
     * Tích hợp theo cơ chế VietQR thông qua SePay API.
     */
    @PostMapping("/createPayment")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody PaymentRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        String qrUrl = paymentService.createSePayPayment(request.bookingId(), userId);
        return ResponseEntity.status(201)
                .body(new PaymentResponse(
                        qrUrl,
                        "Vui lòng quét mã QR để hoàn tất thanh toán.",
                        request.bookingId(),
                        paymentService.getReferenceCode(request.bookingId()),
                        paymentService.getBankName(),
                        paymentService.getAccountNumber(),
                        paymentService.getAccountName()
                ));
    }

    /**
     * Tra cứu danh sách giao dịch thanh toán của một booking theo path `/api/getPayments/booking/{bookingId}`.
     */
    @GetMapping("/getPayments/booking/{bookingId}")
    public ResponseEntity<List<PaymentHistoryResponse>> getPaymentsByBooking(
            @PathVariable("bookingId") Long bookingId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(paymentService.getPaymentsByBookingId(bookingId, userId, userRole));
    }

    /**
     * Tra cứu danh sách giao dịch thanh toán của một booking theo query `/api/getPayments/byBooking?bookingId=...`.
     */
    @GetMapping("/getPayments/byBooking")
    public ResponseEntity<List<PaymentHistoryResponse>> getPaymentsByBookingParam(
            @RequestParam("bookingId") Long bookingId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(paymentService.getPaymentsByBookingId(bookingId, userId, userRole));
    }
}
