package com.ttcsn.venue.exception;

import com.ttcsn.common.exception.NotFoundException;

public class SeatNotFoundException extends NotFoundException {
    public SeatNotFoundException(Long seatId) {
        super("Không tìm thấy ghế với seatId = " + seatId);
    }
}
