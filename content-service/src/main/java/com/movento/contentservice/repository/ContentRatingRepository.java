package com.movento.contentservice.repository;

import com.movento.contentservice.model.Content;
import com.movento.contentservice.model.ContentRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContentRatingRepository extends BaseRepository<ContentRating, Long> {
    
    @Query("SELECT AVG(cr.rating) FROM ContentRating cr WHERE cr.content.id = :contentId")
    Double getAverageRatingByContentId(@Param("contentId") Long contentId);
    
    @Query("SELECT COUNT(cr) FROM ContentRating cr WHERE cr.content.id = :contentId")
    Long getRatingCountByContentId(@Param("contentId") Long contentId);
    
    Page<ContentRating> findByUserId(Long userId, Pageable pageable);
    
    Page<ContentRating> findByContentId(Long contentId, Pageable pageable);
    
    Optional<ContentRating> findByContentIdAndUserId(Long contentId, Long userId);
    
    Page<ContentRating> findByContentIdAndUserId(Long contentId, Long userId, Pageable pageable);
    
    boolean existsByContentIdAndUserId(Long contentId, Long userId);
    
    @Query("SELECT cr FROM ContentRating cr WHERE cr.content.id = :contentId ORDER BY cr.updatedAt DESC")
    Page<ContentRating> findLatestRatingsByContentId(@Param("contentId") Long contentId, Pageable pageable);
}
