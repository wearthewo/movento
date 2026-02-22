package com.movento.contentservice.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ViewHistoryRequest {
    @NotNull(message = "Content ID is required")
    private Long contentId;
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    private Integer progressSeconds;
    
    private Boolean completed = false;
}
