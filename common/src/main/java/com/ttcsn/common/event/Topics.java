/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.common.event;
/**
 *
 * @author quanghieu
 */
public final class Topics {
    private Topics() {}

    // showtime-service
    public static final String SEAT_HELD       = "seat.held";      
    public static final String SEAT_RELEASED   = "seat.released";   
    public static final String SEAT_CONFIRMED  = "seat.confirmed";  

    // booking-service
    public static final String BOOKING_CREATED    = "booking.created";
    public static final String BOOKING_CONFIRMED  = "booking.confirmed";
    public static final String BOOKING_CANCELLED  = "booking.cancelled";

    // payment-service 
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED    = "payment.failed";

    // user-service
    public static final String USER_REGISTERED = "user.registered";
}