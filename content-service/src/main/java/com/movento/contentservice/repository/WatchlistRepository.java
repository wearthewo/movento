package com.movento.contentservice.repository;
import com.movento.contentservice.model.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface WatchlistRepository extends JpaRepository<WatchlistItem,Long> { List<WatchlistItem> findByProfileIdOrderByCreatedAtDesc(UUID profileId); Optional<WatchlistItem> findByProfileIdAndContentId(UUID profileId,Long contentId); }
