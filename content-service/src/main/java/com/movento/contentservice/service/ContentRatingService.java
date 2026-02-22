package com.movento.contentservice.service;

import com.movento.contentservice.dto.ContentRatingDto;
import com.movento.contentservice.dto.request.ContentRatingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContentRatingService {
    ContentRatingDto createRating(ContentRatingRequest request);
    ContentRatingDto getRatingById(Long id);
    Page<ContentRatingDto> getAllRatings(Long contentId, Long userId, Pageable pageable);
    ContentRatingDto updateRating(Long id, ContentRatingRequest request);
    void deleteRating(Long id);
}