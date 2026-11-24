package com.movento.recommendationservice.controller;

import com.movento.recommendationservice.dto.RecommendationRequest;
import com.movento.recommendationservice.dto.RecommendationResponse;
import com.movento.recommendationservice.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

/**
 * Viewers may only read their own recommendations. Writes and cross-account reads are admin-only.
 * X-Account-Id / X-User-Roles are set by the gateway from a verified JWT (client copies are stripped).
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationCrudController {

    private static final String ACCOUNT = "X-Account-Id";
    private static final String ROLES = "X-User-Roles";

    private final RecommendationService service;

    @GetMapping
    public List<RecommendationResponse> recommendations(@RequestHeader(ACCOUNT) Long accountId) {
        return service.getUserRecommendations(accountId);
    }

    @GetMapping("/{id}")
    public RecommendationResponse get(@RequestHeader(ACCOUNT) Long accountId,
                                      @RequestHeader(value = ROLES, required = false) String roles,
                                      @PathVariable Long id) {
        RecommendationResponse recommendation = service.getRecommendation(id);
        if (!isAdmin(roles) && !accountId.equals(recommendation.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return recommendation;
    }

    @GetMapping("/user/{userId}")
    public List<RecommendationResponse> getUserRecommendations(@RequestHeader(ACCOUNT) Long accountId,
                                                               @RequestHeader(value = ROLES, required = false) String roles,
                                                               @PathVariable Long userId) {
        if (!isAdmin(roles) && !accountId.equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return service.getUserRecommendations(userId);
    }

    @PostMapping
    public RecommendationResponse create(@RequestHeader(value = ROLES, required = false) String roles,
                                         @Valid @RequestBody RecommendationRequest request) {
        requireAdmin(roles);
        return service.createRecommendation(request);
    }

    @PostMapping("/{id}/score")
    public RecommendationResponse updateScore(@RequestHeader(value = ROLES, required = false) String roles,
                                              @PathVariable Long id, @RequestBody Double score) {
        requireAdmin(roles);
        return service.updateScore(id, score);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(value = ROLES, required = false) String roles, @PathVariable Long id) {
        requireAdmin(roles);
        service.deleteRecommendation(id);
        return ResponseEntity.noContent().build();
    }

    private static boolean isAdmin(String roles) {
        return roles != null && Arrays.stream(roles.split(",")).map(String::trim).anyMatch("ROLE_ADMIN"::equals);
    }

    private static void requireAdmin(String roles) {
        if (!isAdmin(roles)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
