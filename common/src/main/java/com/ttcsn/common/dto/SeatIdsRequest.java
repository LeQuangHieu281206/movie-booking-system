package com.ttcsn.common.dto;

import java.util.List;

/**
 * Request giữ/đặt ghế: booking-service -> movie-service (internal), hoặc giữ chỗ từ frontend.
 * seatIds là id ghế vật lý của venue (bảng seats) — showtime_seats chỉ phát sinh bản ghi
 * khi có tương tác (HOLDING khi giữ chỗ, BOOKED khi chốt vé); ghế chưa có bản ghi = AVAILABLE.
 */
public record SeatIdsRequest(
        Long showtimeId,
        List<Long> seatIds,
        String holdBy
) {}
