package com.movento.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ViewerProfileRequest(
        @NotBlank @Size(max = 40) String name,
        @Size(max = 512) String avatarUrl,
        boolean kidsMode,
        @Pattern(regexp = "KIDS|TEEN|ADULT", message = "must be KIDS, TEEN or ADULT") String maturityLevel) {}
