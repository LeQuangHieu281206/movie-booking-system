package com.ttcsn.movie.showtime.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Yêu cầu giữ/bỏ giữ 1 ghế từ frontend (mô hình showtime_seats phát sinh theo tương tác).
 * holdBy do server lấy từ X-User-Id, không nhận từ client.
 */
public record ShowtimeSeatInteractionRequest(
        @NotNull(message = "showtimeId không được để trống")
        Long showtimeId,

        @NotNull(message = "seatId không được để trống")
        Long seatId
) {
}
