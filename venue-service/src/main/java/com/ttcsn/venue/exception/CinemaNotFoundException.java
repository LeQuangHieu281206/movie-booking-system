package com.ttcsn.venue.exception;

import com.ttcsn.common.exception.NotFoundException;

public class CinemaNotFoundException extends NotFoundException {
    public CinemaNotFoundException(Long cinemaId) {
        super("Không tìm thấy rạp với cinemaId = " + cinemaId);
    }
    public CinemaNotFoundException(String message) {
        super(message);
    }
}
