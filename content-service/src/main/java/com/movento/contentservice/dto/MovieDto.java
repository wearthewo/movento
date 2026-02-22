package com.movento.contentservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieDto extends ContentDto {
    private String director;
    private Double imdbRating;
    private Double boxOfficeRevenue;
}
