package com.movento.contentservice.repository;

import com.movento.contentservice.model.PlaybackProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaybackProgressRepository extends JpaRepository<PlaybackProgress, Long> {
    List<PlaybackProgress> findTop20ByProfileIdOrderByUpdatedAtDesc(UUID profileId);

    Optional<PlaybackProgress> findByProfileIdAndContentIdAndEpisodeKey(UUID profileId, Long contentId, String episodeKey);

    Optional<PlaybackProgress> findFirstByProfileIdAndContentIdOrderByUpdatedAtDesc(UUID profileId, Long contentId);

    /**
     * Atomic insert-or-update. The player saves on time updates, pause and exit, so two saves for a brand-new
     * resume point can arrive together; a read-then-insert would hit the unique key and fail with a 500.
     */
    @Modifying
    @Query(value = """
            INSERT INTO playback_progress (profile_id, content_id, episode_key, progress_seconds, duration_seconds, completed, updated_at)
            VALUES (:profileId, :contentId, :episodeKey, :progressSeconds, :durationSeconds, :completed, CURRENT_TIMESTAMP)
            ON CONFLICT (profile_id, content_id, episode_key) DO UPDATE SET
              progress_seconds = EXCLUDED.progress_seconds,
              duration_seconds = EXCLUDED.duration_seconds,
              completed = EXCLUDED.completed,
              updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    int upsert(@Param("profileId") UUID profileId, @Param("contentId") Long contentId, @Param("episodeKey") String episodeKey,
               @Param("progressSeconds") int progressSeconds, @Param("durationSeconds") int durationSeconds,
               @Param("completed") boolean completed);
}
