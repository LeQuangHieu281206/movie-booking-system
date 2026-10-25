package com.ttcsn.common.exception;

/** Base cho 401 UNAUTHORIZED (sai thông tin đăng nhập, token sai/hết hạn, ...). */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
