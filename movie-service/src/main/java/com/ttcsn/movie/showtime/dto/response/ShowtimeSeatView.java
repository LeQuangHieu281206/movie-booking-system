/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.movie.showtime.dto.response;

import java.time.OffsetDateTime;
/**
 *
 * @author quanghieu
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