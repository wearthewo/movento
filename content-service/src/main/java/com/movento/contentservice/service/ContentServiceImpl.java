package com.movento.contentservice.service;

import com.movento.contentservice.dto.ContentDto;
import com.movento.contentservice.dto.mapper.ContentMapper;
import com.movento.contentservice.dto.request.MovieRequest;
import com.movento.contentservice.dto.request.TvShowRequest;
import com.movento.contentservice.exceptions.ResourceNotFoundException;
import com.movento.contentservice.model.Content;
import com.movento.contentservice.model.Genre;
import com.movento.contentservice.model.Movie;
import com.movento.contentservice.model.TvShow;
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

    @Override
    @Transactional
    public Content createMovie(MovieRequest movieRequest, List<Long> genreIds) {
        Movie movie = new Movie();
        mapContentRequestToContent(movieRequest, movie);
        movie.setDirector(movieRequest.getDirector());
        movie.setImdbRating(movieRequest.getImdbRating());
        movie.setBoxOfficeRevenue(movieRequest.getBoxOfficeRevenue() != null ? 
            movieRequest.getBoxOfficeRevenue().doubleValue() : null);
        
        if (genreIds != null && !genreIds.isEmpty()) {
            Set<Genre> genres = new HashSet<>(genreRepository.findAllById(genreIds));
            movie.setGenres(genres);
        }
        
        return contentRepository.save(movie);
    }

    @Override
    @Transactional
    public Content updateMovie(Long id, MovieRequest movieRequest, List<Long> genreIds) {
        return contentRepository.findById(id).map(existingContent -> {
            if (!(existingContent instanceof Movie movie)) {
                throw new IllegalArgumentException("Content with id " + id + " is not a movie");
            }
            
            mapContentRequestToContent(movieRequest, movie);
            movie.setDirector(movieRequest.getDirector());
            movie.setImdbRating(movieRequest.getImdbRating());
            movie.setBoxOfficeRevenue(movieRequest.getBoxOfficeRevenue() != null ? 
                movieRequest.getBoxOfficeRevenue().doubleValue() : null);
            
            if (genreIds != null) {
                Set<Genre> genres = new HashSet<>(genreRepository.findAllById(genreIds));
                movie.setGenres(genres);
            }
            
            return contentRepository.save(movie);
        }).orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));
    }

    @Override
    @Transactional
    public Content createTvShow(TvShowRequest tvShowRequest, List<Long> genreIds) {
        TvShow tvShow = new TvShow();
        mapContentRequestToContent(tvShowRequest, tvShow);
        tvShow.setNumberOfSeasons(tvShowRequest.getNumberOfSeasons());
        tvShow.setNumberOfEpisodes(tvShowRequest.getNumberOfEpisodes());
        tvShow.setIsOngoing(tvShowRequest.getIsOngoing());
        
        if (genreIds != null && !genreIds.isEmpty()) {
            Set<Genre> genres = new HashSet<>(genreRepository.findAllById(genreIds));
            tvShow.setGenres(genres);
        }
        
        return contentRepository.save(tvShow);
    }

    @Override
    @Transactional
    public Content updateTvShow(Long id, TvShowRequest tvShowRequest, List<Long> genreIds) {
        return contentRepository.findById(id).map(existingContent -> {
            if (!(existingContent instanceof TvShow tvShow)) {
                throw new IllegalArgumentException("Content with id " + id + " is not a TV show");
            }
            
            mapContentRequestToContent(tvShowRequest, tvShow);
            tvShow.setNumberOfSeasons(tvShowRequest.getNumberOfSeasons());
            tvShow.setNumberOfEpisodes(tvShowRequest.getNumberOfEpisodes());
            tvShow.setIsOngoing(tvShowRequest.getIsOngoing());
            
            if (genreIds != null) {
                Set<Genre> genres = new HashSet<>(genreRepository.findAllById(genreIds));
                tvShow.setGenres(genres);
            }
            
            return contentRepository.save(tvShow);
        }).orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));
    }

    private void mapContentRequestToContent(com.movento.contentservice.dto.request.ContentRequest request, Content content) {
        content.setTitle(request.getTitle());
        content.setDescription(request.getDescription());
        content.setReleaseYear(request.getReleaseYear());
        content.setDurationMinutes(request.getDurationMinutes());
        content.setThumbnailUrl(request.getThumbnailUrl());
        content.setBackdropUrl(request.getBackdropUrl());
        content.setContentRating(request.getContentRating());
    }
}
