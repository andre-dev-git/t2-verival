package com.codequests.checkout.shared.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {
    private String errorCode;
    private String message;
    private List<FieldValidationError> fieldErrors;

    public ApiErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.fieldErrors = List.of();
    }
}

