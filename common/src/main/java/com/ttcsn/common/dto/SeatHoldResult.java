package com.ttcsn.common.dto;

import java.util.List;

/** Kết quả giữ/trả ghế: movie-service -> booking-service (internal). */
public record SeatHoldResult(
        boolean success,
        List<Long> unavailableSeatIds,
        String message
) {
    public static SeatHoldResult ok() {
        return new SeatHoldResult(true, List.of(), "OK");
    }
}
