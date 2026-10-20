package com.ttcsn.movie.showtime.dto.response;

/**
 * View ghế theo suất chiếu (bảng showtime_seats) — phục vụ sơ đồ ghế
 * và luồng test Postman: lấy showtimeSeatIds AVAILABLE để createBooking.
 *
 * seatRow/seatNumber lấy tư vị venue-service? Không — lưu tôn trọng id ghế;
 * chi tiết hàng/cột nằm ở venue (seats). Ở đây trả seatId + status là đủ với booking.
 */
public record ShowtimeSeatView(
        Long showtimeSeatId,
        Long showtimeId,
        Long seatId,
        String status   // AVAILABLE | HELD | BOOKED
) {}
