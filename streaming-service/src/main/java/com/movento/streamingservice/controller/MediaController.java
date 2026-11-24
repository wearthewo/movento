package com.movento.streamingservice.controller;

import com.movento.streamingservice.dto.StreamingSessionResponse;
import com.movento.streamingservice.media.MediaSigner;
import com.movento.streamingservice.model.StreamingSession;
import com.movento.streamingservice.service.StreamingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Validates a signed manifest URL and redirects to the media origin. In production the origin would be a
 * CDN/object store that also checks signatures for segments; locally it is a public HLS test stream.
 */
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {
    private final MediaSigner signer;
    private final StreamingSessionService sessions;

    @Value("${app.media.base-url}")
    private String mediaBaseUrl;

    @Value("${app.media.default-manifest}")
    private String defaultManifest;

    @GetMapping("/{sessionId}/{assetId}/master.m3u8")
    public ResponseEntity<Void> manifest(@PathVariable long sessionId, @PathVariable long assetId,
                                         @RequestParam long exp, @RequestParam String sig) {
        if (!signer.verify(sessionId, assetId, exp, sig)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        StreamingSessionResponse session;
        try {
            session = sessions.getSession(sessionId);
        } catch (IllegalArgumentException missing) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (session.getStatus() != StreamingSession.SessionStatus.ACTIVE || session.getContentId() == null || session.getContentId() != assetId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String origin = mediaBaseUrl.endsWith("/") ? mediaBaseUrl + defaultManifest : mediaBaseUrl + "/" + defaultManifest;
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(origin)).header("Cache-Control", "no-store").build();
    }
}
