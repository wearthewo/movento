package com.movento.streamingservice.dto;

import com.movento.streamingservice.model.StreamingSession;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class StreamingSessionResponse {
    private Long id;
    private Long userId;
    private Long contentId;
    private StreamingSession.SessionStatus status;
    private Instant startedAt;
    private Instant endedAt;
}
