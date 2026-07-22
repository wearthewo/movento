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
    
    @Query("SELECT c FROM Content c LEFT JOIN c.ratings r GROUP BY c ORDER BY AVG(r.rating) DESC")
    Page<Content> findAllOrderByAverageRatingDesc(Pageable pageable);

    Optional<Content> findBySlugAndActiveTrue(String slug);
    Page<Content> findByActiveTrue(Pageable pageable);
    Page<Content> findByActiveTrueAndTitleContainingIgnoreCase(String query, Pageable pageable);
    List<Content> findTop12ByActiveTrueAndTrendingTrueOrderByCreatedAtDesc();
    List<Content> findTop12ByActiveTrueOrderByCreatedAtDesc();
    Optional<Content> findFirstByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();

    @Query("""
        SELECT c FROM Content c
        LEFT JOIN c.genres g
        WHERE c.active = true
          AND (
            LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(c.contentRating) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(g.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR CAST(c.releaseYear AS string) = :query
          )
        ORDER BY
          CASE
            WHEN LOWER(c.title) = LOWER(:query) THEN 0
            WHEN LOWER(c.title) LIKE LOWER(CONCAT(:query, '%')) THEN 1
            WHEN LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) THEN 2
            ELSE 3
          END,
          c.trending DESC,
          c.releaseYear DESC,
          c.title ASC
        """)
    List<Content> searchCatalog(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT c FROM Content c WHERE c.id IN :ids")
    List<Content> findByIds(@Param("ids") List<Long> ids);
    
    @Query("SELECT c FROM Content c WHERE c.id = :id AND TYPE(c) = :contentType")
    <T extends Content> Optional<T> findByIdAndType(@Param("id") Long id, @Param("contentType") Class<T> contentType);
    
    boolean existsByTitle(String title);
}
