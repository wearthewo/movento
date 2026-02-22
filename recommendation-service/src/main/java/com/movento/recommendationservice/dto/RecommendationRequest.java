package com.movento.recommendationservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendationRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long contentId;

    @NotNull
    private Double score;
}
