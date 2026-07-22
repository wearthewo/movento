package com.movento.contentservice.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "movies")
@PrimaryKeyJoinColumn(name = "id")
public class Movie extends Content {
    
    @Column(length = 100)
    private String director;
    
    @Column(name = "imdb_rating", precision = 3, scale = 1)
    private BigDecimal imdbRating;
    
    @Column(name = "box_office_revenue", precision = 15, scale = 2)
    private BigDecimal boxOfficeRevenue;
}
