package com.movento.recommendationservice.controller;

import com.movento.recommendationservice.dto.RecommendationRequest;
import com.movento.recommendationservice.dto.RecommendationResponse;
import com.movento.recommendationservice.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class RecommendationCrudController {

    private final RecommendationService service;

    @PostMapping
    public ResponseEntity<RecommendationResponse> create(@Valid @RequestBody RecommendationRequest request) {
        return ResponseEntity.ok(service.createRecommendation(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecommendationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getRecommendation(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecommendationResponse>> getUserRecommendations(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getUserRecommendations(userId));
    }

    @PostMapping("/{id}/score")
    public ResponseEntity<RecommendationResponse> updateScore(@PathVariable Long id,
                                                              @RequestBody Double score) {
        return ResponseEntity.ok(service.updateScore(id, score));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteRecommendation(id);
        return ResponseEntity.noContent().build();
    }
}
