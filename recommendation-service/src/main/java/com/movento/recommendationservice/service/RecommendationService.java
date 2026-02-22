package com.movento.recommendationservice.service;

import com.movento.recommendationservice.dto.RecommendationRequest;
import com.movento.recommendationservice.dto.RecommendationResponse;
import com.movento.recommendationservice.model.Recommendation;
import com.movento.recommendationservice.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.messaging.exchange}")
    private String exchange;
    @Value("${app.messaging.routing-key}")
    private String routingKey;

    @Cacheable(value = "recommendations", key = "#id")
    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendation(Long id) {
        Recommendation recommendation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found"));
        return mapToResponse(recommendation);
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getUserRecommendations(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RecommendationResponse createRecommendation(RecommendationRequest request) {
        Recommendation recommendation = new Recommendation();
        recommendation.setUserId(request.getUserId());
        recommendation.setContentId(request.getContentId());
        recommendation.setScore(request.getScore());
        Recommendation saved = repository.save(recommendation);
        publishEvent("RECOMMENDATION_CREATED", saved);
        return mapToResponse(saved);
    }

    @CacheEvict(value = "recommendations", key = "#id")
    @Transactional
    public RecommendationResponse updateScore(Long id, Double score) {
        Recommendation recommendation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found"));
        recommendation.setScore(score);
        Recommendation saved = repository.save(recommendation);
        publishEvent("RECOMMENDATION_UPDATED", saved);
        return mapToResponse(saved);
    }

    @CacheEvict(value = "recommendations", key = "#id")
    @Transactional
    public void deleteRecommendation(Long id) {
        Recommendation recommendation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found"));
        repository.delete(recommendation);
        publishEvent("RECOMMENDATION_DELETED", recommendation);
    }

    private RecommendationResponse mapToResponse(Recommendation recommendation) {
        return RecommendationResponse.builder()
                .id(recommendation.getId())
                .userId(recommendation.getUserId())
                .contentId(recommendation.getContentId())
                .score(recommendation.getScore())
                .createdAt(recommendation.getCreatedAt())
                .build();
    }

    private void publishEvent(String type, Recommendation recommendation) {
        String message = String.format("%s:%s:%s", type, recommendation.getId(), recommendation.getUserId());
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
