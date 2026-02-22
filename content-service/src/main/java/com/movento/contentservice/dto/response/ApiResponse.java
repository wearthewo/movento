package com.movento.contentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private LocalDateTime timestamp;
    private int status;
    private String message;
    private T data;
    private String path;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CREATED.value())
                .message("Created successfully")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, HttpStatus status, String path) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .message(message)
                .path(path)
                .build();
    }

    public static <T> ApiResponse<T> notFound(String message, String path) {
        return error(message, HttpStatus.NOT_FOUND, path);
    }

    public static <T> ApiResponse<T> badRequest(String message, String path) {
        return error(message, HttpStatus.BAD_REQUEST, path);
    }

    public static <T> ApiResponse<T> unauthorized(String message, String path) {
        return error(message, HttpStatus.UNAUTHORIZED, path);
    }

    public static <T> ApiResponse<T> forbidden(String message, String path) {
        return error(message, HttpStatus.FORBIDDEN, path);
    }

    public static <T> ApiResponse<T> internalServerError(String message, String path) {
        return error(message, HttpStatus.INTERNAL_SERVER_ERROR, path);
    }
}
