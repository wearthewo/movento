package com.movento.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ViewerProfileRequest(@NotBlank @Size(max = 50) String name, String avatarUrl, boolean kidsMode, String maturityLevel) {}
