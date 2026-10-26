/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.movie.showtime.exception;

import com.ttcsn.common.exception.NotFoundException;
/**
 *
 * @author quanghieu
 */
public class ShowtimeSeatNotFoundException extends NotFoundException {
    public ShowtimeSeatNotFoundException(Long showtimeSeatId) {
        super("Không tìm thấy ghế trong suất chiếu với showtimeSeatId = " + showtimeSeatId);
    }
    public ShowtimeSeatNotFoundException(String message) {
        super(message);
    }
}