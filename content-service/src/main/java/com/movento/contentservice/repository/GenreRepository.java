package com.movento.contentservice.repository;

import com.movento.contentservice.model.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface GenreRepository extends BaseRepository<Genre, Long> {
    
    Optional<Genre> findByName(String name);
    
    @Query("SELECT g FROM Genre g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Genre> searchByName(@Param("query") String query);
    
    @Query("SELECT g FROM Genre g JOIN g.contents c WHERE c.id = :contentId")
    List<Genre> findByContentId(@Param("contentId") Long contentId);
    
    boolean existsByName(String name);

    Page<Genre> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
