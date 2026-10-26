/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.common.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
/**
 *
 * @author quanghieu
 */
public record PaymentCompletedEvent(
        
        UUID paymentId,
        
        Long bookingId,
        
        Long userId,
        
        BigDecimal amount,
        
        OffsetDateTime paymentTime,
        
        String status
) {
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED  = "FAILED";

    public boolean isSuccessful() {
        return STATUS_SUCCESS.equalsIgnoreCase(this.status);
    }
}