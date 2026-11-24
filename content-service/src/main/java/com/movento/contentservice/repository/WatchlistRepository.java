package com.movento.contentservice.repository;

import com.movento.contentservice.model.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findByProfileIdOrderByCreatedAtDesc(UUID profileId);

    Optional<WatchlistItem> findByProfileIdAndContentId(UUID profileId, Long contentId);

    /** Idempotent add: a double click (or two tabs) can never violate the unique key. */
    @Modifying
    @Query(value = "INSERT INTO watchlist_items (profile_id, content_id, created_at) VALUES (:profileId, :contentId, CURRENT_TIMESTAMP) "
            + "ON CONFLICT (profile_id, content_id) DO NOTHING", nativeQuery = true)
    int addIfAbsent(@Param("profileId") UUID profileId, @Param("contentId") Long contentId);
}
