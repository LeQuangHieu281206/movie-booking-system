package com.ttcsn.movie.showtime.exception;

import com.ttcsn.common.exception.NotFoundException;

public class ShowtimeSeatNotFoundException extends NotFoundException {
    public ShowtimeSeatNotFoundException(Long showtimeSeatId) {
        super("Không tìm thấy ghế trong suất chiếu với showtimeSeatId = " + showtimeSeatId);
    }
    public ShowtimeSeatNotFoundException(String message) {
        super(message);
    }
}
