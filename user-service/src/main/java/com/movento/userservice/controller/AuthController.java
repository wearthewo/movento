package com.movento.userservice.controller;

import com.movento.userservice.dto.JwtResponse;
import com.movento.userservice.dto.LoginRequest;
import com.movento.userservice.dto.SignupRequest;
import com.movento.userservice.dto.RefreshTokenRequest;
import com.movento.userservice.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        return ResponseEntity.ok(authService.registerUser(signUpRequest));
    }

    @PostMapping("/refresh") public ResponseEntity<JwtResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) { return ResponseEntity.ok(authService.refresh(request.refreshToken())); }
    @PostMapping("/logout") public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) { authService.logout(request.refreshToken()); return ResponseEntity.noContent().build(); }
}
