package com.movento.contentservice.repository;

import com.movento.contentservice.model.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContentRepository extends BaseRepository<Content, Long> {
    
    @Query("SELECT c FROM Content c WHERE TYPE(c) = :contentType")
    Page<Content> findByContentType(@Param("contentType") Class<? extends Content> contentType, Pageable pageable);
    
    @Query("SELECT c FROM Content c JOIN c.genres g WHERE g.id = :genreId")
    Page<Content> findByGenreId(@Param("genreId") Long genreId, Pageable pageable);
    
    Page<Content> findByTitleContainingIgnoreCase(String query, Pageable pageable);
    
    Page<Content> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT c FROM Content c ORDER BY c.averageRating DESC NULLS LAST")
    Page<Content> findAllOrderByAverageRatingDesc(Pageable pageable);
    
    @Query("SELECT c FROM Content c WHERE c.id IN :ids")
    List<Content> findByIds(@Param("ids") List<Long> ids);
    
    @Query("SELECT c FROM Content c WHERE c.id = :id AND TYPE(c) = :contentType")
    <T extends Content> Optional<T> findByIdAndType(@Param("id") Long id, @Param("contentType") Class<T> contentType);
    
    boolean existsByTitle(String title);
}
