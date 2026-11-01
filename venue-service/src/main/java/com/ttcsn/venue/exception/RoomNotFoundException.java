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
public class RoomNotFoundException extends NotFoundException {
    public RoomNotFoundException(Long roomId) {
        super("Không tìm thấy phòng chiếu với roomId = " + roomId);
    }
}