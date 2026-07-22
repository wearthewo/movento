package com.movento.userservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class JwtResponse {
    private String accessToken;
    private String type = "Bearer";
    private Long id;
    private String email;
    private List<String> roles;
    private String refreshToken;
    private long expiresIn = 900;

    public JwtResponse(String accessToken, Long id, String email, List<String> roles) {
        this.accessToken = accessToken;
        this.id = id;
        this.email = email;
        this.roles = roles;
    }
}
