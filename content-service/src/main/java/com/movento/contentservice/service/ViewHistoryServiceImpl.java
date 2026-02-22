package com.movento.contentservice.service;

import com.movento.contentservice.client.UserServiceClient;
import com.movento.contentservice.dto.UserDto;
import com.movento.contentservice.dto.ViewHistoryDto;
import com.movento.contentservice.dto.mapper.ViewHistoryMapper;
import com.movento.contentservice.dto.request.ViewHistoryRequest;
import com.movento.contentservice.exception.ResourceNotFoundException;
import com.movento.contentservice.model.Content;
import com.movento.contentservice.model.ViewHistory;
import com.movento.contentservice.repository.ContentRepository;
import com.movento.contentservice.repository.ViewHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewHistoryServiceImpl implements ViewHistoryService {

    private final ViewHistoryRepository viewHistoryRepository;
    private final ContentRepository contentRepository;
    private final UserServiceClient userServiceClient;
    private final ViewHistoryMapper viewHistoryMapper;

    @Override
    @Transactional
    public ViewHistoryDto addToHistory(ViewHistoryRequest request) {
        // First, verify the content exists
        Content content = contentRepository.findById(request.getContentId())
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + request.getContentId()));

        try {
            // Verify user exists using the user-service
            UserDto userDto = userServiceClient.getUserById(request.getUserId());
            if (userDto == null) {
                throw new ResourceNotFoundException("User not found with id: " + request.getUserId());
            }

            // Check if there's an existing view history for this user and content
            Optional<ViewHistory> existingHistory = viewHistoryRepository.findByContentIdAndUserId(
                    request.getContentId(), request.getUserId());

            ViewHistory viewHistory;
            if (existingHistory.isPresent()) {
                viewHistory = existingHistory.get();
                viewHistory.setViewedAt(LocalDateTime.now());
                if (request.getProgressSeconds() != null) {
                    // Convert seconds to minutes (rounding down)
                    int minutes = request.getProgressSeconds() / 60;
                    viewHistory.updateProgress(minutes);
                } else if (request.getCompleted() != null && request.getCompleted()) {
                    viewHistory.markAsCompleted();
                }
            } else {
                viewHistory = viewHistoryMapper.toEntity(request);
                viewHistory.setContent(content);
                viewHistory.setCompleted(request.getCompleted() != null && request.getCompleted());
            }

            ViewHistory savedHistory = viewHistoryRepository.save(viewHistory);
            return viewHistoryMapper.toDto(savedHistory);
            
        } catch (Exception e) {
            log.error("Error adding to view history for user id: {} and content id: {}", 
                    request.getUserId(), request.getContentId(), e);
            throw new RuntimeException("Failed to add to view history: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ViewHistoryDto getHistoryById(Long id) {
        return viewHistoryRepository.findById(id)
                .map(viewHistoryMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("View history not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViewHistoryDto> getViewHistory(Long userId, Long contentId, Pageable pageable) {
        if (userId != null && contentId != null) {
            return viewHistoryRepository.findByUserIdAndContentId(userId, contentId, pageable)
                    .map(viewHistoryMapper::toDto);
        } else if (userId != null) {
            return viewHistoryRepository.findByUserIdOrderByLastWatchedAtDesc(userId, pageable)
                    .map(viewHistoryMapper::toDto);
        } else if (contentId != null) {
            return viewHistoryRepository.findByContentId(contentId, pageable)
                    .map(viewHistoryMapper::toDto);
        }
        return viewHistoryRepository.findAll(pageable)
                .map(viewHistoryMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViewHistoryDto> getRecentWatched(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "viewedAt"));
        return viewHistoryRepository.findByUserIdOrderByLastWatchedAtDesc(userId, pageable)
                .map(viewHistoryMapper::toDto);
    }

    @Override
    @Transactional
    public void removeFromHistory(Long id) {
        if (!viewHistoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("View history not found with id: " + id);
        }
        viewHistoryRepository.deleteById(id);
    }
}