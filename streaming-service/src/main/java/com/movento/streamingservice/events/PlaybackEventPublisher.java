package com.movento.streamingservice.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends playback events to RabbitMQ only after the session row has committed, so consumers never
 * hear about a session that was rolled back. Publishing is best effort: a broker outage is logged
 * and never breaks playback (a transactional outbox would make delivery guaranteed).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaybackEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.messaging.exchange}")
    private String exchange;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publish(PlaybackEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, event.type(), event);
            log.info("Published {} event={} session={} account={} profile={} content={}",
                    event.type(), event.eventId(), event.sessionId(), event.accountId(), event.profileId(), event.contentId());
        } catch (AmqpException brokerUnavailable) {
            log.warn("Could not publish {} for session {}: {}", event.type(), event.sessionId(), brokerUnavailable.getMessage());
        }
    }
}
