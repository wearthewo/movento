package com.movento.streamingservice.service;

import com.movento.streamingservice.dto.StreamingSessionRequest;
import com.movento.streamingservice.dto.StreamingSessionResponse;
import com.movento.streamingservice.model.StreamingSession;
import com.movento.streamingservice.repository.StreamingSessionRepository;
import com.movento.streamingservice.events.PlaybackEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StreamingSessionService {

    private final StreamingSessionRepository repository;
    /** Events are handed to PlaybackEventPublisher, which sends them to RabbitMQ after commit. */
    private final ApplicationEventPublisher events;

    @Cacheable(value = "streamingSessions", key = "#id")
    @Transactional(readOnly = true)
    public StreamingSessionResponse getSession(Long id) {
        StreamingSession session = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        return mapToResponse(session);
    }

    @Transactional(readOnly = true)
    public List<StreamingSessionResponse> getUserSessions(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StreamingSessionResponse createSession(StreamingSessionRequest request) {
        StreamingSession session = new StreamingSession();
        session.setUserId(request.getUserId());
        session.setContentId(request.getContentId());
        session.setStatus(StreamingSession.SessionStatus.ACTIVE);
        session.setStartedAt(Instant.now());
        StreamingSession saved = repository.save(session);
        events.publishEvent(PlaybackEvent.of(PlaybackEvent.STARTED, saved.getId(), saved.getUserId(), request.getProfileId(), saved.getContentId()));
        return mapToResponse(saved);
    }

    @CacheEvict(value = "streamingSessions", key = "#id")
    @Transactional
    public StreamingSessionResponse endSession(Long id) {
        StreamingSession session = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        session.setStatus(StreamingSession.SessionStatus.ENDED);
        session.setEndedAt(Instant.now());
        StreamingSession saved = repository.save(session);
        events.publishEvent(PlaybackEvent.of(PlaybackEvent.ENDED, saved.getId(), saved.getUserId(), null, saved.getContentId()));
        return mapToResponse(saved);
    }

    private StreamingSessionResponse mapToResponse(StreamingSession session) {
        return StreamingSessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .contentId(session.getContentId())
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .build();
    }
}
