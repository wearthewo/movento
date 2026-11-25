package com.movento.recommendationservice.events;

import com.movento.recommendationservice.signals.ViewingEventStore;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaybackEventListenerTest {
    private final ViewingEventStore store = mock(ViewingEventStore.class);
    private final PlaybackEventListener listener = new PlaybackEventListener(store);

    private static PlaybackEvent event(UUID id, String profileId, Long contentId) {
        return new PlaybackEvent(id, "playback.started", 1, Instant.parse("2026-11-25T10:00:00Z"), 7L, 42L, profileId, contentId);
    }

    @Test
    void storesAViewingSignalForTheProfile() {
        UUID id = UUID.randomUUID();
        UUID profile = UUID.randomUUID();
        when(store.save(any(), anyLong(), any(), anyLong(), any())).thenReturn(true);

        listener.onPlaybackStarted(event(id, profile.toString(), 9L));

        verify(store).save(eq(id), eq(42L), eq(profile), eq(9L), eq(Instant.parse("2026-11-25T10:00:00Z")));
    }

    @Test
    void redeliveredEventIsHandledWithoutError() {
        UUID id = UUID.randomUUID();
        when(store.save(any(), anyLong(), any(), anyLong(), any())).thenReturn(true, false);

        listener.onPlaybackStarted(event(id, null, 9L));
        listener.onPlaybackStarted(event(id, null, 9L));

        verify(store, times(2)).save(eq(id), eq(42L), isNull(), eq(9L), any());
    }

    @Test
    void malformedEventIsRejectedForTheDeadLetterQueue() {
        assertThatThrownBy(() -> listener.onPlaybackStarted(event(UUID.randomUUID(), null, null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verify(store, never()).save(any(), anyLong(), any(), anyLong(), any());
    }

    @Test
    void unknownProfileFormatIsTreatedAsNoProfile() {
        assertThat(PlaybackEventListener.parseProfile("default")).isNull();
        assertThat(PlaybackEventListener.parseProfile(null)).isNull();
    }
}
