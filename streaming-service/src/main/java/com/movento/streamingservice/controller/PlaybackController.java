package com.movento.streamingservice.controller;

import com.movento.streamingservice.dto.StreamingSessionRequest;
import com.movento.streamingservice.dto.StreamingSessionResponse;
import com.movento.streamingservice.service.StreamingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

@RestController @RequestMapping("/api/v1/playback") @RequiredArgsConstructor
public class PlaybackController {
    private final StreamingSessionService sessions;
    @Value("${app.media.base-url}") private String mediaBaseUrl;
    @Value("${app.media.default-manifest}") private String defaultManifest;
    @Value("${app.media.require-subscription:false}") private boolean requireSubscription;

    @PostMapping("/{assetId}/session")
    public ResponseEntity<Map<String, Object>> create(@PathVariable Long assetId,
            @RequestHeader("X-Account-Id") Long accountId,
            @RequestHeader(value = "X-Profile-Id", required = false) String profileId,
            @RequestHeader(value = "X-Subscription-Status", required = false) String subscription) {
        if (requireSubscription && !("ACTIVE".equals(subscription) || "TRIALING".equals(subscription))) return ResponseEntity.status(402).build();
        StreamingSessionRequest request = new StreamingSessionRequest(); request.setUserId(accountId); request.setContentId(assetId);
        StreamingSessionResponse session = sessions.createSession(request);
        String manifest = URI.create(mediaBaseUrl.endsWith("/") ? mediaBaseUrl + defaultManifest : mediaBaseUrl + "/" + defaultManifest).toString();
        return ResponseEntity.ok(Map.of("sessionId", session.getId(), "manifestUrl", manifest, "expiresAt", Instant.now().plusSeconds(900).toString(), "profileId", profileId == null ? "default" : profileId));
    }

    @PostMapping("/sessions/{id}/complete") public StreamingSessionResponse complete(@PathVariable Long id) { return sessions.endSession(id); }
}
