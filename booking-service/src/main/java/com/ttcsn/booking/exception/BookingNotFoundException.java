package com.ttcsn.booking.exception;

import com.ttcsn.common.exception.NotFoundException;

public class BookingNotFoundException extends NotFoundException {
    public BookingNotFoundException(Long bookingId) {
        super("Không tìm thấy đơn đặt vé với bookingId = " + bookingId);
    }
    public BookingNotFoundException(String message) {
        super(message);
    }
}
