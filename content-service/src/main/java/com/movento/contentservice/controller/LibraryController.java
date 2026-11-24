package com.movento.contentservice.controller;

import com.movento.contentservice.catalog.CatalogService;
import com.movento.contentservice.catalog.MaturityPolicy;
import com.movento.contentservice.dto.CatalogTitleResponse;
import com.movento.contentservice.model.PlaybackProgress;
import com.movento.contentservice.model.WatchlistItem;
import com.movento.contentservice.repository.ContentRepository;
import com.movento.contentservice.repository.PlaybackProgressRepository;
import com.movento.contentservice.repository.WatchlistRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Per-profile library: My List, resume points and Continue Watching. X-Profile-Id is set by the gateway. */
@RestController
@RequestMapping("/api/v1/library")
@RequiredArgsConstructor
public class LibraryController {
    private static final String PROFILE = "X-Profile-Id";
    private static final String MATURITY = "X-Profile-Maturity";
    private static final double COMPLETED_RATIO = 0.92;

    private final WatchlistRepository watchlist;
    private final PlaybackProgressRepository progress;
    private final ContentRepository content;
    private final CatalogService catalog;

    public record ProgressRequest(@Min(0) @Max(86_400) int progressSeconds,
                                  @Min(1) @Max(86_400) int durationSeconds,
                                  @Size(max = 100) String episodeKey) {}

    public record ProgressItem(Long contentId, String episodeKey, int progressSeconds, int durationSeconds,
                               boolean completed, int progressPercent, Instant updatedAt) {
        static ProgressItem of(PlaybackProgress p) {
            int percent = (int) Math.min(100, Math.round(100.0 * p.getProgressSeconds() / Math.max(1, p.getDurationSeconds())));
            return new ProgressItem(p.getContent().getId(), p.getEpisodeKey(), p.getProgressSeconds(), p.getDurationSeconds(),
                    p.isCompleted(), percent, p.getUpdatedAt());
        }
    }

    public record ContinueWatchingItem(CatalogTitleResponse title, ProgressItem progress) {}

    @GetMapping("/watchlist")
    @Transactional(readOnly = true)
    public List<Long> watchlist(@RequestHeader(PROFILE) UUID profileId) {
        return watchlist.findByProfileIdOrderByCreatedAtDesc(profileId).stream().map(i -> i.getContent().getId()).toList();
    }

    /** My List with full title cards, newest first, filtered for the profile's maturity. */
    @GetMapping("/watchlist/titles")
    @Transactional(readOnly = true)
    public List<CatalogTitleResponse> watchlistTitles(@RequestHeader(PROFILE) UUID profileId,
                                                      @RequestHeader(value = MATURITY, required = false) String maturity) {
        return catalog.byIds(MaturityPolicy.fromHeader(maturity), watchlist(profileId));
    }

    @GetMapping("/watchlist/{contentId}")
    @Transactional(readOnly = true)
    public Map<String, Boolean> inWatchlist(@RequestHeader(PROFILE) UUID profileId, @PathVariable Long contentId) {
        return Map.of("saved", watchlist.findByProfileIdAndContentId(profileId, contentId).isPresent());
    }

    @PutMapping("/watchlist/{contentId}")
    @Transactional
    public ResponseEntity<Void> add(@RequestHeader(PROFILE) UUID profileId, @PathVariable Long contentId) {
        if (watchlist.findByProfileIdAndContentId(profileId, contentId).isEmpty()) {
            WatchlistItem item = new WatchlistItem();
            item.setProfileId(profileId);
            item.setContent(content.findById(contentId).orElseThrow());
            watchlist.save(item);
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/watchlist/{contentId}")
    @Transactional
    public ResponseEntity<Void> remove(@RequestHeader(PROFILE) UUID profileId, @PathVariable Long contentId) {
        watchlist.findByProfileIdAndContentId(profileId, contentId).ifPresent(watchlist::delete);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/progress")
    @Transactional(readOnly = true)
    public List<ProgressItem> progress(@RequestHeader(PROFILE) UUID profileId) {
        return progress.findTop20ByProfileIdOrderByUpdatedAtDesc(profileId).stream().map(ProgressItem::of).toList();
    }

    /** Resume point for one title (latest episode for series). 204 when never started. */
    @GetMapping("/progress/{contentId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ProgressItem> progressFor(@RequestHeader(PROFILE) UUID profileId, @PathVariable Long contentId) {
        return progress.findFirstByProfileIdAndContentIdOrderByUpdatedAtDesc(profileId, contentId)
                .map(ProgressItem::of).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Netflix "Continue Watching": started, not finished, one entry per title, most recent first. */
    @GetMapping("/continue-watching")
    @Transactional(readOnly = true)
    public List<ContinueWatchingItem> continueWatching(@RequestHeader(PROFILE) UUID profileId,
                                                       @RequestHeader(value = MATURITY, required = false) String maturity) {
        Map<Long, ProgressItem> latest = new java.util.LinkedHashMap<>();
        progress.findTop20ByProfileIdOrderByUpdatedAtDesc(profileId).stream()
                .filter(p -> !p.isCompleted() && p.getProgressSeconds() > 30)
                .forEach(p -> latest.putIfAbsent(p.getContent().getId(), ProgressItem.of(p)));
        return catalog.byIds(MaturityPolicy.fromHeader(maturity), List.copyOf(latest.keySet())).stream()
                .map(title -> new ContinueWatchingItem(title, latest.get(Long.valueOf(title.id()))))
                .toList();
    }

    @PutMapping("/progress/{contentId}")
    @Transactional
    public ResponseEntity<Void> update(@RequestHeader(PROFILE) UUID profileId, @PathVariable Long contentId,
                                       @Valid @RequestBody ProgressRequest request) {
        String episode = request.episodeKey() == null ? "" : request.episodeKey();
        PlaybackProgress item = progress.findByProfileIdAndContentIdAndEpisodeKey(profileId, contentId, episode)
                .orElseGet(PlaybackProgress::new);
        item.setProfileId(profileId);
        item.setContent(content.findById(contentId).orElseThrow());
        item.setEpisodeKey(episode);
        item.setDurationSeconds(request.durationSeconds());
        item.setProgressSeconds(Math.min(request.progressSeconds(), request.durationSeconds()));
        item.setCompleted(item.getProgressSeconds() >= (int) (item.getDurationSeconds() * COMPLETED_RATIO));
        item.setUpdatedAt(Instant.now());
        progress.save(item);
        return ResponseEntity.noContent().build();
    }
}
