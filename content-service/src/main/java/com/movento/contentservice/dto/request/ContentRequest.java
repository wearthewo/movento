package com.movento.contentservice.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.movento.contentservice.dto.GenreDto;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = MovieRequest.class, name = "MOVIE"),
    @JsonSubTypes.Type(value = TvShowRequest.class, name = "TV_SHOW")
})
public abstract class ContentRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;
    
    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;
    
    @Positive(message = "Release year must be a positive number")
    private Integer releaseYear;
    
    @Positive(message = "Duration must be a positive number")
    private Integer durationMinutes;
    
    @Size(max = 512, message = "Thumbnail URL must be less than 512 characters")
    private String thumbnailUrl;
    
    @Size(max = 512, message = "Backdrop URL must be less than 512 characters")
    private String backdropUrl;
    
    @Size(max = 10, message = "Content rating must be less than 10 characters")
    private String contentRating;
    
    @NotNull(message = "Genres cannot be null")
    private Set<GenreDto> genres = new HashSet<>();
}
