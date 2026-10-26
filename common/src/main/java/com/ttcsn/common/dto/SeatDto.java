/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.common.dto;
/**
 *
 * @author quanghieu
 */
public record SeatDto(
        Long seatId,
        Long roomId,
        String seatRow,
        Short seatNumber,
        String seatType
) {}