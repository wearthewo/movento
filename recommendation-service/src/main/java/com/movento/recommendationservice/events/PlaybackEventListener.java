package com.movento.recommendationservice.events;

import com.movento.recommendationservice.signals.ViewingEventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Turns playback.started events into viewing signals.
 * <ul>
 *   <li>Idempotent: a redelivered event (same eventId) is stored once.</li>
 *   <li>Malformed events are rejected without requeue and end up in the dead-letter queue.</li>
 *   <li>Transient failures (e.g. database down) are retried, then dead-lettered (see application.yml).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaybackEventListener {
    private final ViewingEventStore store;

    @RabbitListener(queues = MessagingConfig.QUEUE)
    public void onPlaybackStarted(PlaybackEvent event) {
        if (event == null || event.eventId() == null || event.accountId() == null || event.contentId() == null) {
            log.warn("Rejecting malformed playback event (will be dead-lettered): {}", event);
            throw new AmqpRejectAndDontRequeueException("Malformed playback event: " + event);
        }
        if (event.version() > 1) {
            log.warn("Playback event {} has newer version {}; reading known fields only", event.eventId(), event.version());
        }
        boolean stored = store.save(event.eventId(), event.accountId(), parseProfile(event.profileId()), event.contentId(),
                event.occurredAt() == null ? Instant.now() : event.occurredAt());
        if (stored) {
            log.info("Consumed {} event={} profile={} content={} -> viewing signal stored",
                    event.type(), event.eventId(), event.profileId(), event.contentId());
        } else {
            log.info("Ignored duplicate {} event={} (already stored)", event.type(), event.eventId());
        }
    }

    public static UUID parseProfile(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException notAUuid) {
            return null;
        }
    }
}
