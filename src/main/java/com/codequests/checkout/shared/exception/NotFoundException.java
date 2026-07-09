package com.codequests.checkout.shared.exception;

public class NotFoundException extends BusinessException {
    public NotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}

