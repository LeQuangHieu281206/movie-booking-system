package com.ttcsn.movie.exception;

import com.ttcsn.common.exception.NotFoundException;

public class GenreNotFoundException extends NotFoundException {
    public GenreNotFoundException(Long genreId) {
        super("Không tìm thấy thể loại với genreId = " + genreId);
    }
    public GenreNotFoundException(String message) {
        super(message);
    }
}
