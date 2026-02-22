package com.movento.paymentservice.exception;

import com.movento.paymentservice.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentProcessingException(PaymentProcessingException ex, WebRequest request) {
        log.error("Payment processing error: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, ex.getHttpStatus(), request, ex.getErrorCode());
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentException(PaymentException ex, WebRequest request) {
        log.error("Payment error: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : ""
                ));

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                        .path(request.getDescription(false).replace("uri=", ""))
                        .timestamp(LocalDateTime.now())
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String field = violation.getPropertyPath().toString();
            errors.put(field.substring(field.lastIndexOf('.') + 1), violation.getMessage());
        });

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                        .path(request.getDescription(false).replace("uri=", ""))
                        .timestamp(LocalDateTime.now())
                        .data(errors)
                        .build());
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        MethodArgumentTypeMismatchException.class,
        MissingRequestHeaderException.class,
        MissingServletRequestParameterException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequestExceptions(Exception ex, WebRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.warn("Method not allowed: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.METHOD_NOT_ALLOWED, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllUncaughtException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
            Exception ex, HttpStatus status, WebRequest request) {
        return buildErrorResponse(ex, status, request, status.getReasonPhrase().toLowerCase().replace(" ", "_"));
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
            Exception ex, HttpStatus status, WebRequest request, String errorCode) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .status(status.value())
                        .error(errorCode)
                        .path(request.getDescription(false).replace("uri=", ""))
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
