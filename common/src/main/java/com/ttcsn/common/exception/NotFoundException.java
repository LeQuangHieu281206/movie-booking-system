package com.ttcsn.common.exception;

/** Base cho 404 — mọi NOT_FOUND exception của từng service kế thừa class này. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
