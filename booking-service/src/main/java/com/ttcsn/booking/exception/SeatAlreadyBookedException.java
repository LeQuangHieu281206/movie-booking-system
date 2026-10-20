package com.ttcsn.booking.exception;

import com.ttcsn.common.exception.ConflictException;

public class SeatAlreadyBookedException extends ConflictException {
    public SeatAlreadyBookedException(Long showtimeSeatId) {
        super("Ghế đã được đặt hoặc đang được giữ, showtimeSeatId = " + showtimeSeatId);
    }
    public SeatAlreadyBookedException(String message) {
        super(message);
    }
}
