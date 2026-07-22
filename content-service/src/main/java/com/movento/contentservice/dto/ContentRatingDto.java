package com.movento.contentservice.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Getter
@Setter
public class ContentRatingDto extends BaseDto {
    @NotNull
    private Long contentId;
    
    @NotNull
    private Long userId;
    
    @NotNull
    @DecimalMin("1.0")
    @DecimalMax("10.0")
    @Digits(integer = 2, fraction = 1)
    private BigDecimal rating;
    
    @Size(max = 2000)
    private String review;
    
    // Response fields
    private String userName;
    private String userAvatar;
}
