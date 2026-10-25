package com.ttcsn.user.exception;

import com.ttcsn.common.exception.UnauthorizedException;

/** Token sai chữ ký, sai cấu hình, sai loại (access/refresh) hoặc đã hết hạn → 401. */
public class InvalidTokenException extends UnauthorizedException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
