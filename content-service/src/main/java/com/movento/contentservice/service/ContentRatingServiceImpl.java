package com.movento.contentservice.service;

import com.movento.contentservice.client.UserServiceClient;
import com.movento.contentservice.dto.ContentRatingDto;
import com.movento.contentservice.dto.UserDto;
import com.movento.contentservice.dto.mapper.ContentRatingMapper;
import com.movento.contentservice.dto.request.ContentRatingRequest;
import com.movento.contentservice.exception.ResourceNotFoundException;
import com.movento.contentservice.model.Content;
import com.movento.contentservice.model.ContentRating;
import com.movento.contentservice.repository.ContentRatingRepository;
import com.movento.contentservice.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentRatingServiceImpl implements ContentRatingService {

    private final ContentRatingRepository contentRatingRepository;
    private final ContentRepository contentRepository;
    private final UserServiceClient userServiceClient;
    private final ContentRatingMapper contentRatingMapper;

    @Override
    @Transactional
    public ContentRatingDto createRating(ContentRatingRequest request) {
        // Verify content exists
        Content content = contentRepository.findById(request.getContentId())
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + request.getContentId()));

        // Verify user exists by calling user-service
        try {
            UserDto user = userServiceClient.getUserById(request.getUserId());
            if (user == null) {
                throw new ResourceNotFoundException("User not found with id: " + request.getUserId());
            }
        } catch (Exception e) {
            throw new ResourceNotFoundException("Error verifying user: " + e.getMessage());
        }

        // Create and save the rating
        ContentRating rating = contentRatingMapper.toEntity(request);
        rating.setContent(content);
        // No need to set user as it's just an ID in the entity

        ContentRating savedRating = contentRatingRepository.save(rating);
        return contentRatingMapper.toDto(savedRating);
    }

    @Override
    @Transactional(readOnly = true)
    public ContentRatingDto getRatingById(Long id) {
        return contentRatingRepository.findById(id)
                .map(contentRatingMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentRatingDto> getAllRatings(Long contentId, Long userId, Pageable pageable) {
        if (contentId != null && userId != null) {
            return contentRatingRepository.findByContentIdAndUserId(contentId, userId, pageable)
                    .map(contentRatingMapper::toDto);
        } else if (contentId != null) {
            return contentRatingRepository.findByContentId(contentId, pageable)
                    .map(contentRatingMapper::toDto);
        } else if (userId != null) {
            return contentRatingRepository.findByUserId(userId, pageable)
                    .map(contentRatingMapper::toDto);
        }
        return contentRatingRepository.findAll(pageable)
                .map(contentRatingMapper::toDto);
    }

    @Override
    @Transactional
    public ContentRatingDto updateRating(Long id, ContentRatingRequest request) {
        ContentRating rating = contentRatingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found with id: " + id));

        contentRatingMapper.updateEntityFromRequest(request, rating);
        ContentRating updatedRating = contentRatingRepository.save(rating);
        return contentRatingMapper.toDto(updatedRating);
    }

    @Override
    @Transactional
    public void deleteRating(Long id) {
        if (!contentRatingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Rating not found with id: " + id);
        }
        contentRatingRepository.deleteById(id);
    }
}