package com.ttcsn.user.exception;

import com.ttcsn.common.exception.UnauthorizedException;

/** Sai email hoặc mật khẩu khi login. Thông báo chung, không tiết lộ email có tồn tại hay không. */
public class InvalidCredentialsException extends UnauthorizedException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
