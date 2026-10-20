package com.ttcsn.common.dto;

import java.util.List;

/** Request giữ ghế: booking-service -> movie-service (internal). */
public record SeatIdsRequest(
        Long showtimeId,
        List<Long> showtimeSeatIds
) {}
