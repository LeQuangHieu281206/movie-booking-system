package com.ttcsn.user.exception;

import com.ttcsn.common.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(Long userId) {
        super("Không tìm thấy người dùng với userId = " + userId);
    }
    public UserNotFoundException(String message) {
        super(message);
    }
}
