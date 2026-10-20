package com.ttcsn.user.exception;

import com.ttcsn.common.exception.ConflictException;

public class EmailAlreadyExistsException extends ConflictException {
    public EmailAlreadyExistsException(String email) {
        super("Email đã được sử dụng: " + email);
    }
}
