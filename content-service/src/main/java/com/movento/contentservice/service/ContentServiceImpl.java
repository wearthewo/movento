package com.movento.contentservice.service;

import com.movento.contentservice.dto.ContentDto;
import com.movento.contentservice.dto.mapper.ContentMapper;
import com.movento.contentservice.exceptions.ResourceNotFoundException;
import com.movento.contentservice.model.Content;
import com.movento.contentservice.model.Genre;
import com.movento.contentservice.repository.ContentRepository;
import com.movento.contentservice.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ContentServiceImpl extends BaseServiceImpl<Content, Long> implements ContentService {

    private final ContentRepository contentRepository;
    private final GenreRepository genreRepository;
    private final ContentMapper contentMapper;
    
    public ContentServiceImpl(ContentRepository contentRepository, 
                            GenreRepository genreRepository,
                            ContentMapper contentMapper) {
        super(contentRepository);
        this.contentRepository = contentRepository;
        this.genreRepository = genreRepository;
        this.contentMapper = contentMapper;
    }

    @Override
    public Page<Content> findByGenreId(Long genreId, Pageable pageable) {
        return contentRepository.findByGenreId(genreId, pageable);
    }

    @Override
    public Page<Content> searchByTitle(String query, Pageable pageable) {
        return contentRepository.findByTitleContainingIgnoreCase(query, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ContentDto getContentDetails(Long id, Long userId) {
        return contentRepository.findById(id)
            .map(content -> contentMapper.toDto(content, userId))
            .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Content> findTopRated(Pageable pageable) {
        return contentRepository.findAllOrderByAverageRatingDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Content> findRecentlyAdded(Pageable pageable) {
        return contentRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Transactional
    public Content createContent(Content content, List<Long> genreIds) {
        if (genreIds != null && !genreIds.isEmpty()) {
            Set<Genre> genres = new HashSet<>(genreRepository.findAllById(genreIds));
            content.setGenres(genres);
        }
        return contentRepository.save(content);
    }

    @Override
    @Transactional
    public Content updateContent(Long id, Content content, List<Long> genreIds) {
        return contentRepository.findById(id).map(existingContent -> {
            content.setId(id);
            if (genreIds != null) {
                Set<Genre> genres = new HashSet<>(genreRepository.findAllById(genreIds));
                existingContent.setGenres(genres);
            }
            // Update other fields as needed
            existingContent.setTitle(content.getTitle());
            existingContent.setDescription(content.getDescription());
            existingContent.setReleaseYear(content.getReleaseYear());
            existingContent.setDurationMinutes(content.getDurationMinutes());
            existingContent.setThumbnailUrl(content.getThumbnailUrl());
            existingContent.setBackdropUrl(content.getBackdropUrl());
            existingContent.setContentRating(content.getContentRating());
            
            return contentRepository.save(existingContent);
        }).orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));
    }
}
