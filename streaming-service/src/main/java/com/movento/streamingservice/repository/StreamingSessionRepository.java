package com.movento.streamingservice.repository;

import com.movento.streamingservice.model.StreamingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreamingSessionRepository extends JpaRepository<StreamingSession, Long> {
    List<StreamingSession> findByUserId(Long userId);
}
