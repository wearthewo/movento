package com.movento.contentservice.controller;

import com.movento.contentservice.dto.ContentRatingDto;
import com.movento.contentservice.dto.request.ContentRatingRequest;
import com.movento.contentservice.dto.response.ApiResponse;
import com.movento.contentservice.service.ContentRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class ContentRatingController {

    private final ContentRatingService contentRatingService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContentRatingDto>> createRating(@Valid @RequestBody ContentRatingRequest request) {
        ContentRatingDto createdRating = contentRatingService.createRating(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdRating.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.created(createdRating));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentRatingDto>> getRatingById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(contentRatingService.getRatingById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ContentRatingDto>>> getAllRatings(
            @RequestParam(required = false) Long contentId,
            @RequestParam(required = false) Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
            contentRatingService.getAllRatings(contentId, userId, pageable)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentRatingDto>> updateRating(
            @PathVariable Long id,
            @Valid @RequestBody ContentRatingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            contentRatingService.updateRating(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRating(@PathVariable Long id) {
        contentRatingService.deleteRating(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
