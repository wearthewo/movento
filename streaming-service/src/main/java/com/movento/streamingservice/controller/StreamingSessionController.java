package com.movento.streamingservice.controller;

import com.movento.streamingservice.dto.StreamingSessionRequest;
import com.movento.streamingservice.dto.StreamingSessionResponse;
import com.movento.streamingservice.service.StreamingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class StreamingSessionController {

    private final StreamingSessionService service;

    @PostMapping
    public ResponseEntity<StreamingSessionResponse> create(@Valid @RequestBody StreamingSessionRequest request) {
        return ResponseEntity.ok(service.createSession(request));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<StreamingSessionResponse> end(@PathVariable Long id) {
        return ResponseEntity.ok(service.endSession(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StreamingSessionResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSession(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<StreamingSessionResponse>> getUserSessions(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getUserSessions(userId));
    }
}
