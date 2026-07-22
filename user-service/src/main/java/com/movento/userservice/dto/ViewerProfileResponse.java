package com.movento.userservice.dto;

import java.util.UUID;
public record ViewerProfileResponse(UUID id, String name, String avatarUrl, boolean kidsMode, String maturityLevel) {}
