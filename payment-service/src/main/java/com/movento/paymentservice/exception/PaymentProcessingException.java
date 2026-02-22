package com.movento.paymentservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an error occurs during payment processing.
 */
public class PaymentProcessingException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;

    public PaymentProcessingException(String message) {
        this(message, "payment_error", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        this(message, "payment_error", HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
    
    public PaymentProcessingException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public PaymentProcessingException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
