/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.common.dto;

import java.math.BigDecimal;
/**
 *
 * @author quanghieu
 */
public record BookingPaymentView(
        Long bookingId,
        Long userId,
        BigDecimal totalAmount,
        String status
) {
}