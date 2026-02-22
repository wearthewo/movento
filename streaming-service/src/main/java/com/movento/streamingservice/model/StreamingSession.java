package com.movento.streamingservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "streaming_sessions")
@Getter
@Setter
@NoArgsConstructor
public class StreamingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long userId;

    @NotNull
    @Column(nullable = false)
    private Long contentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    private Instant endedAt;

    public enum SessionStatus {
        ACTIVE,
        ENDED
    }
}
