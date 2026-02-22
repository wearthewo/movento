package com.movento.recommendationservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class RecommendationResponse {
    private Long id;
    private Long userId;
    private Long contentId;
    private Double score;
    private Instant createdAt;
}
