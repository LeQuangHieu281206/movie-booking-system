package com.ttcsn.payment.entity;

/**
 * Trạng thái giao dịch thanh toán.
 * PENDING  : đã tạo QR, chờ cổng thanh toán xác nhận
 * SUCCESS  : cổng đã xác nhận tiền vào
 * FAILED   : giao dịch không hợp lệ (sai số tiền, booking không tồn tại/không còn PENDING...)
 * CANCELLED: booking đã bị huỷ trước khi thanh toán
 * EXPIRED  : booking đã hết hạn chờ thanh toán
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED,
    EXPIRED
}
