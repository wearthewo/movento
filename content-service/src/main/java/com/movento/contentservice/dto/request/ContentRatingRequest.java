package com.movento.contentservice.dto.request;

import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class ContentRatingRequest {
    @NotNull(message = "Content ID is required")
    private Long contentId;
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Rating is required")
    @DecimalMin(value = "1.0", message = "Rating must be at least 1")
    @DecimalMax(value = "10.0", message = "Rating cannot be more than 10")
    @Digits(integer = 2, fraction = 1, message = "Rating must have up to 2 digits before and 1 after decimal")
    private BigDecimal rating;
    
    @Size(max = 2000, message = "Review cannot exceed 2000 characters")
    private String review;
}
