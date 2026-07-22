package com.movento.streamingservice.controller;

import com.movento.streamingservice.dto.StreamingSessionResponse;
import com.movento.streamingservice.service.StreamingSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaybackControllerTest {
    @Test void returnsConfiguredHlsManifestAndNeverTrustsBrowserUserIds() {
        StreamingSessionService sessions = mock(StreamingSessionService.class);
        when(sessions.createSession(any())).thenReturn(StreamingSessionResponse.builder().id(7L).build());
        PlaybackController controller = new PlaybackController(sessions);
        ReflectionTestUtils.setField(controller, "mediaBaseUrl", "https://media.example/hls");
        ReflectionTestUtils.setField(controller, "defaultManifest", "master.m3u8");
        ReflectionTestUtils.setField(controller, "requireSubscription", false);
        var result = controller.create(9L, 42L, "profile-id", null);
        assertThat(result.getBody()).containsEntry("sessionId", 7L).containsEntry("manifestUrl", "https://media.example/hls/master.m3u8");
    }
}
