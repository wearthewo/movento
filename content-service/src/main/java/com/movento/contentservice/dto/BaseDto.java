package com.movento.contentservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseDto {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
