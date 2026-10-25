package com.ttcsn.movie.showtime.dto.response;

import java.time.OffsetDateTime;

/**
 * View 1 ghế theo suất chiếu cho sơ đồ ghế + đặt vé.
 *
 * <p>Ghế chưa có bản ghi = AVAILABLE (showtimeSeatId null). seatRow/seatNumber/seatType
 * lấy từ venue-service. status: AVAILABLE | HOLDING | BOOKED. Không trả holdBy ra public.
 */
public record ShowtimeSeatView(
        Long showtimeSeatId,
        Long showtimeId,
        Long seatId,
        String seatRow,
        Short seatNumber,
        String seatType,
        String status,
        OffsetDateTime holdUntil
) {}
