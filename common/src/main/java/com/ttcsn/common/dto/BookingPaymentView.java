package com.ttcsn.common.dto;

import java.math.BigDecimal;

/**
 * Internal view booking-service → payment-service.
 * Chỉ chứa dữ liệu payment cần để xác thực; không expose qua gateway.
 */
public record BookingPaymentView(
        Long bookingId,
        Long userId,
        BigDecimal totalAmount,
        String status
) {
}
