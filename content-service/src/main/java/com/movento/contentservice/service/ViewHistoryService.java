package com.movento.contentservice.service;

import com.movento.contentservice.dto.ViewHistoryDto;
import com.movento.contentservice.dto.request.ViewHistoryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ViewHistoryService {
    ViewHistoryDto addToHistory(ViewHistoryRequest request);
    ViewHistoryDto getHistoryById(Long id);
    Page<ViewHistoryDto> getViewHistory(Long userId, Long contentId, Pageable pageable);
    Page<ViewHistoryDto> getRecentWatched(Long userId, int limit);
    void removeFromHistory(Long id);
}