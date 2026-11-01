/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.common.dto;
/**
 *
 * @author quanghieu
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