package com.codequests.checkout.shared.exception;

public class InvalidStateException extends BusinessException {
    public InvalidStateException(String errorCode, String message) {
        super(errorCode, message);
    }
}

