package com.movento.streamingservice.service;

import com.movento.streamingservice.dto.StreamingSessionRequest;
import com.movento.streamingservice.events.PlaybackEvent;
import com.movento.streamingservice.model.StreamingSession;
import com.movento.streamingservice.repository.StreamingSessionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StreamingSessionServiceTest {

    @Test
    void startingASessionPublishesAPlaybackStartedEvent() {
        StreamingSessionRepository repository = mock(StreamingSessionRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        when(repository.save(any())).thenAnswer(invocation -> {
            StreamingSession session = invocation.getArgument(0);
            session.setId(5L);
            return session;
        });

        StreamingSessionRequest request = new StreamingSessionRequest();
        request.setUserId(42L);
        request.setContentId(9L);
        request.setProfileId("2f1c8f7e-8f4b-4f57-9d7e-0f6a5d1f2a10");
        new StreamingSessionService(repository, events).createSession(request);

        ArgumentCaptor<PlaybackEvent> captor = ArgumentCaptor.forClass(PlaybackEvent.class);
        verify(events).publishEvent(captor.capture());
        PlaybackEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo("playback.started");
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.sessionId()).isEqualTo(5L);
        assertThat(event.accountId()).isEqualTo(42L);
        assertThat(event.contentId()).isEqualTo(9L);
        assertThat(event.profileId()).isEqualTo("2f1c8f7e-8f4b-4f57-9d7e-0f6a5d1f2a10");
    }
}
