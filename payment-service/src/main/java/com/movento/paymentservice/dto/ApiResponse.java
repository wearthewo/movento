package com.movento.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private LocalDateTime timestamp = LocalDateTime.now();
    private int status;
    private String message;
    private T data;
    private String path;
    private String error;
    private boolean success;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.status = HttpStatus.OK.value();
        response.message = "Success";
        response.data = data;
        response.success = true;
        return response;
    }

    public static <T> ApiResponse<T> created(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.status = HttpStatus.CREATED.value();
        response.message = "Created successfully";
        response.data = data;
        response.success = true;
        return response;
    }

    public static <T> ApiResponse<T> error(String message, HttpStatus status, String path) {
        ApiResponse<T> response = new ApiResponse<>();
        response.status = status.value();
        response.message = message;
        response.path = path;
        response.success = false;
        return response;
    }

    public ApiResponse<T> setData(T data) {
        this.data = data;
        return this;
    }
}
