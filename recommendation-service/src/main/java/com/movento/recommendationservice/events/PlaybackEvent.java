package com.movento.recommendationservice.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumer-side copy of streaming-service's event contract (version 1). Services share the JSON shape,
 * not a Java library, so each can evolve and deploy independently.
 */
public record PlaybackEvent(UUID eventId, String type, int version, Instant occurredAt,
                            Long sessionId, Long accountId, String profileId, Long contentId) {
}
