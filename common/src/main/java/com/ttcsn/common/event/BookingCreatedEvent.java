package com.ttcsn.common.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * SOA Step 0 — Event khi booking-service phát lệnh tạo booking (Step 5).
 * Chỉ chứa dữ liệu dạng ID + số, KHÔNG chứa Entity của domain khác.
 * Idempotency: sagaId trùng -> consumer bỏ qua.
 */
public record BookingCreatedEvent(
        UUID sagaId,              // idempotency key của Saga
        Long bookingId,           // booking đã tạo với trạng thái PENDING
        String bookingCode,
        Long userId,              // ID thuần (không có Entity User)
        Long showtimeId,          // ID thuần
        List<Long> showtimeSeatIds,
        BigDecimal totalAmount
) {}
