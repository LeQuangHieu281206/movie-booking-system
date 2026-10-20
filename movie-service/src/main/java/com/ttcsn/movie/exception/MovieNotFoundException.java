package com.ttcsn.movie.exception;

import com.ttcsn.common.exception.NotFoundException;

public class MovieNotFoundException extends NotFoundException {
    public MovieNotFoundException(Long movieId) {
        super("Không tìm thấy phim với movieId = " + movieId);
    }
    public MovieNotFoundException(String message) {
        super(message);
    }
}
