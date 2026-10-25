package com.ttcsn.common.exception;

/** Base cho 403 FORBIDDEN (đã xác thực nhưng không đủ quyền). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
