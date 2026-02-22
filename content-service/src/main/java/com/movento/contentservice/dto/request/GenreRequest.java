package com.movento.contentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for creating or updating a Genre
 */
@Data
public class GenreRequest {
    
    @NotBlank(message = "Genre name is required")
    private String name;
    
    private String description;
}
