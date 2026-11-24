package com.movento.streamingservice.controller;

import com.movento.streamingservice.dto.StreamingSessionRequest;
import com.movento.streamingservice.dto.StreamingSessionResponse;
import com.movento.streamingservice.media.EntitlementClient;
import com.movento.streamingservice.media.MediaSigner;
import com.movento.streamingservice.service.StreamingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

/**
 * Starts playback: checks the account's plan, records a session and hands back a short-lived signed
 * manifest URL. The URL is relative to the web app, which proxies /api/media/** to the gateway.
 */
@RestController
@RequestMapping("/api/v1/playback")
@RequiredArgsConstructor
public class PlaybackController {
    private final StreamingSessionService sessions;
    private final EntitlementClient entitlements;
    private final MediaSigner signer;

    @Value("${app.media.require-subscription:true}")
    private boolean requireSubscription;

    @Value("${app.media.url-ttl-seconds:900}")
    private long urlTtlSeconds;

    @PostMapping("/{assetId}/session")
    public ResponseEntity<Map<String, Object>> create(@PathVariable Long assetId,
                                                      @RequestHeader("X-Account-Id") Long accountId,
                                                      @RequestHeader(value = "X-Profile-Id", required = false) String profileId) {
        if (requireSubscription && !entitlements.hasActivePlan(accountId)) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of("message", "Choose a plan to start watching.", "code", "SUBSCRIPTION_REQUIRED"));
        }
        StreamingSessionRequest request = new StreamingSessionRequest();
        request.setUserId(accountId);
        request.setContentId(assetId);
        StreamingSessionResponse session = sessions.createSession(request);
        long expires = Instant.now().getEpochSecond() + urlTtlSeconds;
        String manifestUrl = "/api/media/" + session.getId() + "/" + assetId + "/master.m3u8?exp=" + expires
                + "&sig=" + signer.sign(session.getId(), assetId, expires);
        return ResponseEntity.ok(Map.of("sessionId", session.getId(), "manifestUrl", manifestUrl,
                "expiresAt", Instant.ofEpochSecond(expires).toString(), "profileId", profileId == null ? "default" : profileId));
    }

    /** Only the account that started a session may end it. */
    @PostMapping("/sessions/{id}/complete")
    public StreamingSessionResponse complete(@PathVariable Long id, @RequestHeader("X-Account-Id") Long accountId) {
        if (!accountId.equals(sessions.getSession(id).getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return sessions.endSession(id);
    }
}
