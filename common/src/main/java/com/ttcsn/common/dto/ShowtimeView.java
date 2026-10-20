package com.ttcsn.common.dto;

/**
 * View cho Feign: booking-service đọc phản hồi /api/getShowtimeById của movie-service.
 * Chỉ giữ field cần dùng — anti-corruption. Giải quyết cross-module type mà không cần
 * movie-service expose đúng class của nó.
 */
public record ShowtimeView(
        Long showtimeId,
        Long movieId,
        Long roomId,
        String roomName,
        String roomType,
        Long cinemaId,
        String cinemaName,
        String cinemaAddress,
        String cinemaCity,
        String startTime,
        String endTime,
        java.math.BigDecimal basePrice,
        String status
) {}
