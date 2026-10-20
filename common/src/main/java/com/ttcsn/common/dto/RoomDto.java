package com.ttcsn.common.dto;

/**
 * DTO trao đổi qua Feign: dữ liệu Room (venue-service -> movie-service).
 * Chứa đủ context Cinema để render/response ghép.
 */
public record RoomDto(
        Long roomId,
        String name,
        String roomType,
        Integer totalSeats,
        Long cinemaId,
        String cinemaName,
        String cinemaAddress,
        String cinemaCity
) {}
