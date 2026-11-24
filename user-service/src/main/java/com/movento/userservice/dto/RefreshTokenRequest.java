package com.movento.userservice.dto;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/** profileId is optional: when present the refreshed access token stays scoped to that viewer profile. */
public record RefreshTokenRequest(@NotBlank String refreshToken, UUID profileId) {}
