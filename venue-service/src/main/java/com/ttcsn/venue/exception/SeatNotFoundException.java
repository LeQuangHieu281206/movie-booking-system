/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.venue.exception;

import com.ttcsn.common.exception.NotFoundException;
/**
 *
 * @author quanghieu
 */
public class SeatNotFoundException extends NotFoundException {
    public SeatNotFoundException(Long seatId) {
        super("Không tìm thấy ghế với seatId = " + seatId);
    }
}