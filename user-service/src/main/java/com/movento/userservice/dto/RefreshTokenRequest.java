package com.movento.userservice.dto;
import jakarta.validation.constraints.NotBlank;
public record RefreshTokenRequest(@NotBlank String refreshToken) {}
