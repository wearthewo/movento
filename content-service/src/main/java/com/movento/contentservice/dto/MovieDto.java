package com.movento.contentservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MovieDto extends ContentDto {
    private String director;
    private BigDecimal imdbRating;
    private BigDecimal boxOfficeRevenue;
}
