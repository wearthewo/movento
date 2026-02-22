package com.movento.streamingservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StreamingSessionRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long contentId;
}
