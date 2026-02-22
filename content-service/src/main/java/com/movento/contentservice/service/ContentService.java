package com.movento.contentservice.service;

import com.movento.contentservice.dto.ContentDto;
import com.movento.contentservice.model.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ContentService extends BaseService<Content, Long> {
    Page<Content> findByGenreId(Long genreId, Pageable pageable);
    Page<Content> searchByTitle(String query, Pageable pageable);
    ContentDto getContentDetails(Long id, Long userId);
    Page<Content> findTopRated(Pageable pageable);
    Page<Content> findRecentlyAdded(Pageable pageable);
    Content createContent(Content content, List<Long> genreIds);
    Content updateContent(Long id, Content content, List<Long> genreIds);
}
