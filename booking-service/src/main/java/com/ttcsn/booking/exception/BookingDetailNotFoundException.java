package com.ttcsn.booking.exception;

import com.ttcsn.common.exception.NotFoundException;

public class BookingDetailNotFoundException extends NotFoundException {
    public BookingDetailNotFoundException(Long bookingDetailId) {
        super("Không tìm thấy chi tiết đặt vé với bookingDetailId = " + bookingDetailId);
    }
    public BookingDetailNotFoundException(String message) {
        super(message);
    }
}
