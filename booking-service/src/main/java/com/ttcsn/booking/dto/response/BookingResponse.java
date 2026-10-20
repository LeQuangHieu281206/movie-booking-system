/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.ttcsn.booking.dto.response;

import com.ttcsn.booking.entity.enums.BookingStatus;
import com.ttcsn.booking.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
/**
 *
 * @author quanghieu
 */
public record BookingResponse(
        Long bookingId,
        Long userId,
        Long showtimeId,
        String bookingCode,
        BigDecimal totalAmount,
        BookingStatus status,
        PaymentMethod paymentMethod,
        OffsetDateTime bookingTime
) {
}
