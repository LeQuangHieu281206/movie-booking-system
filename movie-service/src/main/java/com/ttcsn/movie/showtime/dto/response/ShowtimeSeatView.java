package com.ttcsn.movie.showtime.dto.response;

import java.time.OffsetDateTime;

/**
 * View 1 ghế theo suất chiếu cho sơ đồ ghế + đặt vé.
 *
 * Mô hình showtime_seats phát sinh theo tương tác: ghế chưa có bản ghi = AVAILABLE
 * (showtimeSeatId null). seatRow/seatNumber/seatType lấy từ venue-service (bảng seats).
 * status: AVAILABLE | HOLDING | BOOKED; holdBy chỉ có giá trị khi HOLDING của ai đó.
 */
public record ShowtimeSeatView(
        Long showtimeSeatId,
        Long showtimeId,
        Long seatId,
        String seatRow,
        Short seatNumber,
        String seatType,
        String status,
        OffsetDateTime holdUntil,
        String holdBy
) {}
