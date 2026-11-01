/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.ttcsn.movie.showtime.dto.response;

import com.ttcsn.movie.showtime.entity.enums.ShowtimeStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
/**
 *
 * @author quanghieu
 */
public record ShowtimeResponse(
        Long showtimeId,
        Long movieId,
        Long roomId,
        String roomName,
        String roomType,      
        Long cinemaId,
        String cinemaName,
        String cinemaAddress,
        String cinemaCity,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        BigDecimal basePrice,
        ShowtimeStatus status
) {
}