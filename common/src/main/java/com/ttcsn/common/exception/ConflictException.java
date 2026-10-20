package com.ttcsn.common.exception;

/** Base cho 409 CONFLICT (ghế đã đặt, email trùng, ...). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
