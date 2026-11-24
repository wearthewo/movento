package com.movento.streamingservice.controller;

import com.movento.streamingservice.dto.StreamingSessionResponse;
import com.movento.streamingservice.media.EntitlementClient;
import com.movento.streamingservice.media.MediaSigner;
import com.movento.streamingservice.model.StreamingSession;
import com.movento.streamingservice.service.StreamingSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaybackControllerTest {
    private final StreamingSessionService sessions = mock(StreamingSessionService.class);
    private final EntitlementClient entitlements = mock(EntitlementClient.class);
    private final MediaSigner signer = new MediaSigner("test-signing-secret-1234567890");

    private PlaybackController controller(boolean requireSubscription) {
        PlaybackController controller = new PlaybackController(sessions, entitlements, signer);
        ReflectionTestUtils.setField(controller, "requireSubscription", requireSubscription);
        ReflectionTestUtils.setField(controller, "urlTtlSeconds", 900L);
        return controller;
    }

    @Test
    void returnsSignedExpiringManifestUrl() {
        when(entitlements.hasActivePlan(42L)).thenReturn(true);
        when(sessions.createSession(any())).thenReturn(StreamingSessionResponse.builder().id(7L).build());
        var result = controller(true).create(9L, 42L, "profile-id");
        String url = (String) result.getBody().get("manifestUrl");
        assertThat(url).startsWith("/api/media/7/9/master.m3u8?exp=").contains("&sig=");
        long exp = Long.parseLong(url.replaceAll(".*exp=(\\d+).*", "$1"));
        String sig = url.replaceAll(".*sig=", "");
        assertThat(signer.verify(7L, 9L, exp, sig)).isTrue();
        assertThat(signer.verify(7L, 10L, exp, sig)).as("signature is bound to the title").isFalse();
    }

    @Test
    void refusesPlaybackWithoutActivePlan() {
        when(entitlements.hasActivePlan(42L)).thenReturn(false);
        var result = controller(true).create(9L, 42L, null);
        assertThat(result.getStatusCode().value()).isEqualTo(402);
        verify(sessions, never()).createSession(any());
    }

    @Test
    void mediaRedirectRejectsExpiredOrForgedLinks() {
        MediaController media = new MediaController(signer, sessions);
        ReflectionTestUtils.setField(media, "mediaBaseUrl", "https://media.example/hls");
        ReflectionTestUtils.setField(media, "defaultManifest", "master.m3u8");
        when(sessions.getSession(7L)).thenReturn(StreamingSessionResponse.builder().id(7L).contentId(9L).status(StreamingSession.SessionStatus.ACTIVE).build());

        long future = Instant.now().getEpochSecond() + 60;
        var ok = media.manifest(7L, 9L, future, signer.sign(7L, 9L, future));
        assertThat(ok.getStatusCode().value()).isEqualTo(302);
        assertThat(ok.getHeaders().getLocation()).hasToString("https://media.example/hls/master.m3u8");

        long past = Instant.now().getEpochSecond() - 1;
        assertThat(media.manifest(7L, 9L, past, signer.sign(7L, 9L, past)).getStatusCode().value()).isEqualTo(403);
        assertThat(media.manifest(7L, 9L, future, "forged").getStatusCode().value()).isEqualTo(403);
    }
}
