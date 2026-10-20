package com.ttcsn.movie.showtime.exception;

import com.ttcsn.common.exception.NotFoundException;

public class ShowtimeNotFoundException extends NotFoundException {
    public ShowtimeNotFoundException(Long showtimeId) {
        super("Không tìm thấy suất chiếu với showtimeId = " + showtimeId);
    }
    public ShowtimeNotFoundException(String message) {
        super(message);
    }
}
