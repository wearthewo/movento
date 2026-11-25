package com.movento.streamingservice.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Versioned event contract published to the {@code streaming.events} topic exchange.
 * The routing key equals {@link #type()}; consumers deserialize into their own copy of this record.
 *
 * @param eventId   unique per event, lets consumers ignore redeliveries (idempotency key)
 * @param profileId viewer profile, null when playback was started without a profile-scoped token
 */
public record PlaybackEvent(UUID eventId, String type, int version, Instant occurredAt,
                            Long sessionId, Long accountId, String profileId, Long contentId) {

    public static final String STARTED = "playback.started";
    public static final String ENDED = "playback.ended";
    public static final int VERSION = 1;

    public static PlaybackEvent of(String type, Long sessionId, Long accountId, String profileId, Long contentId) {
        return new PlaybackEvent(UUID.randomUUID(), type, VERSION, Instant.now(), sessionId, accountId, profileId, contentId);
    }
}
