package com.movento.contentservice.dto.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ContentRatingRequest {
    @NotNull(message = "Content ID is required")
    private Long contentId;
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 10, message = "Rating cannot be more than 10")
    private Double rating;
    
    @Size(max = 2000, message = "Review cannot exceed 2000 characters")
    private String review;
}
