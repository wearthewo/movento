package com.movento.contentservice.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Getter
@Setter
@JsonTypeName("MOVIE")
public class MovieRequest extends ContentRequest {
    
    @Size(max = 100, message = "Director name must be less than 100 characters")
    private String director;
    
    @DecimalMin(value = "0.0", message = "IMDb rating must be at least 0.0")
    @DecimalMax(value = "10.0", message = "IMDb rating must be at most 10.0")
    @Digits(integer = 2, fraction = 1, message = "IMDb rating must have up to 2 digits before and 1 after decimal")
    private Double imdbRating;
    
    @PositiveOrZero(message = "Box office revenue must be a positive number or zero")
    @Digits(integer = 15, fraction = 2, message = "Box office revenue must have up to 15 digits before and 2 after decimal")
    private BigDecimal boxOfficeRevenue;
}
