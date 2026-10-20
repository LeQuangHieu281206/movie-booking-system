package com.ttcsn.common.dto;

/** DTO trao đổi ghế vật lý (venue-service -> movie-service khi seed showtime_seats). */
public record SeatDto(
        Long seatId,
        Long roomId,
        String seatRow,
        Short seatNumber,
        String seatType
) {}
