/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.ttcsn.venue.dto.response;

import com.ttcsn.venue.entity.enums.SeatType;
/**
 *
 * @author quanghieu
 */
public record SeatResponse(
        Long id,
        String seatRow,
        Short seatNumber,
        SeatType seatType
) {

}