/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.common.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
/**
 *
 * @author quanghieu
 */
public record BookingCreatedEvent(
        UUID sagaId,             
        Long bookingId,         
        String bookingCode,
        Long userId,            
        Long showtimeId,          
        List<Long> showtimeSeatIds,
        BigDecimal totalAmount
) {}