package com.ttcsn.booking.exception;

import com.ttcsn.common.exception.ConflictException;

public class SeatAlreadyBookedException extends ConflictException {
    public SeatAlreadyBookedException(Long seatId) {
        super("Ghế đã được đặt hoặc đang được giữ, seatId = " + seatId);
    }
    public SeatAlreadyBookedException(String message) {
        super(message);
    }
}
