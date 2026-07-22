package com.movento.contentservice.model;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "content_ratings")
public class ContentRating extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(length = 1000)
    private String review;


}
