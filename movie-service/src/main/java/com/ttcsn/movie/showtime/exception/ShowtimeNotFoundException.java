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
public class ShowtimeNotFoundException extends NotFoundException {
    public ShowtimeNotFoundException(Long showtimeId) {
        super("Không tìm thấy suất chiếu với showtimeId = " + showtimeId);
    }
    public ShowtimeNotFoundException(String message) {
        super(message);
    }
}