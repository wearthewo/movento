package com.movento.contentservice.repository;

import com.movento.contentservice.model.Content;
import com.movento.contentservice.model.ViewHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ViewHistoryRepository extends BaseRepository<ViewHistory, Long> {
    
    Page<ViewHistory> findByUserIdOrderByLastWatchedAtDesc(Long userId, Pageable pageable);
    
    Optional<ViewHistory> findByContentIdAndUserId(Long contentId, Long userId);
    
    Page<ViewHistory> findByContentId(Long contentId, Pageable pageable);
    
    Page<ViewHistory> findByUserIdAndContentId(Long userId, Long contentId, Pageable pageable);
    
    @Query("SELECT vh FROM ViewHistory vh WHERE vh.userId = :userId AND vh.content.id = :contentId AND vh.episode IS NOT NULL")
    List<ViewHistory> findEpisodeHistory(@Param("userId") Long userId, @Param("contentId") Long contentId);
    
    @Query("SELECT vh FROM ViewHistory vh WHERE vh.userId = :userId AND vh.content.id IN :contentIds")
    List<ViewHistory> findByUserIdAndContentIdIn(@Param("userId") Long userId, @Param("contentIds") List<Long> contentIds);
    
    @Modifying
    @Query("DELETE FROM ViewHistory vh WHERE vh.userId = :userId AND vh.content.id = :contentId")
    void deleteByUserIdAndContentId(@Param("userId") Long userId, @Param("contentId") Long contentId);
    
    @Modifying
    @Query("UPDATE ViewHistory vh SET vh.isCompleted = true, vh.progressSeconds = vh.content.durationMinutes * 60 WHERE vh.id = :id")
    void markAsCompleted(@Param("id") Long id);
}
