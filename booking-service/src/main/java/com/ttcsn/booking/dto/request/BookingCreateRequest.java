/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.ttcsn.booking.dto.request;

import com.ttcsn.booking.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
/**
 *
 * @author quanghieu
 */
public record BookingCreateRequest(
        @NotNull(message = "userId không được để trống")
        Long userId,

        @NotNull(message = "showtimeId không được để trống")
        Long showtimeId,

        @NotEmpty(message = "Danh sách ghế đặt không được để trống")
        List<Long> seatIds,

        /** Định danh phiên giữ ghế của client (mô hình showtime_seats phát sinh theo tương tác). */
        String holdBy,

        @NotNull(message = "Tổng tiền không được để trống")
        @Positive(message = "Tổng tiền phải lớn hơn 0")
        BigDecimal totalAmount,

        @NotNull(message = "Phương thức thanh toán không được để trống")
        PaymentMethod paymentMethod
) {
}