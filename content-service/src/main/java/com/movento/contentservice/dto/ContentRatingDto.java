package com.movento.contentservice.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class ContentRatingDto extends BaseDto {
    @NotNull
    private Long contentId;
    
    @NotNull
    private Long userId;
    
    @NotNull
    @Min(1)
    @Max(10)
    private Double rating;
    
    @Size(max = 2000)
    private String review;
    
    // Response fields
    private String userName;
    private String userAvatar;
}
