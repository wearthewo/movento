package com.movento.contentservice.repository;
import com.movento.contentservice.model.PlaybackProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface PlaybackProgressRepository extends JpaRepository<PlaybackProgress,Long> { List<PlaybackProgress> findTop20ByProfileIdOrderByUpdatedAtDesc(UUID profileId); Optional<PlaybackProgress> findByProfileIdAndContentIdAndEpisodeKey(UUID profileId,Long contentId,String episodeKey); }
