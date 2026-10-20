package com.ttcsn.common.event;

import java.util.List;
import java.util.UUID;

/**
 * SOA Step 0 — Lệnh giữ/trả ghế (seat inventory thuộc showtime-service).
 */
public record SeatHoldRequestedEvent(
        UUID sagaId,
        Long showtimeId,
        List<Long> showtimeSeatIds,
        long ttlSeconds
) {}
