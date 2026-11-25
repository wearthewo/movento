package com.movento.recommendationservice.signals;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Behavioural signals built from playback events. The catalog (titles, genres, maturity filtering) stays in
 * content-service: this service answers "what did this profile watch" and "what is popular", and the web
 * app combines that with content-service's similarity ranking.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class ViewingSignalsController {
    private final ViewingEventStore store;

    public record BecauseYouWatched(Long seedContentId, List<Long> watchedContentIds) {}

    @GetMapping("/because-you-watched")
    public BecauseYouWatched becauseYouWatched(@RequestHeader(value = "X-Profile-Id", required = false) String profileHeader) {
        UUID profileId = com.movento.recommendationservice.events.PlaybackEventListener.parseProfile(profileHeader);
        if (profileId == null) return new BecauseYouWatched(null, List.of());
        return new BecauseYouWatched(store.latestContent(profileId).orElse(null), store.watchedContent(profileId, 50));
    }

    @GetMapping("/popular")
    public List<ViewingEventStore.Popular> popular(@RequestParam(defaultValue = "7") int days,
                                                   @RequestParam(defaultValue = "10") int limit) {
        return store.popular(Duration.ofDays(Math.max(1, Math.min(days, 30))), Math.max(1, Math.min(limit, 50)));
    }
}
