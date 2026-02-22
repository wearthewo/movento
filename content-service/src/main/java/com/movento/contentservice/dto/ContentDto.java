package com.movento.contentservice.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = MovieDto.class, name = "MOVIE"),
    @JsonSubTypes.Type(value = TvShowDto.class, name = "TV_SHOW")
})
public abstract class ContentDto extends BaseDto {
    private String title;
    private String description;
    private Integer releaseYear;
    private Integer durationMinutes;
    private String thumbnailUrl;
    private String backdropUrl;
    private String contentRating;
    private boolean active;
    private List<GenreDto> genres = new ArrayList<>();
    private Double averageRating;
    private Long ratingCount;
    private ViewHistoryDto viewHistory;
}
