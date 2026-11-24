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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.movento.userservice.account.AccountSecurityService;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private AccountSecurityService accountSecurity;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        return ResponseEntity.ok(authService.registerUser(signUpRequest));
    }

    @PostMapping("/refresh") public ResponseEntity<JwtResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) { return ResponseEntity.ok(authService.refresh(request.refreshToken(), request.profileId())); }
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@Valid @RequestBody TokenRequest request) {
        accountSecurity.verifyEmail(request.token());
        return ResponseEntity.ok(Map.of("verified", true));
    }

    /** Always 202 so the endpoint cannot be used to discover registered emails. */
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody EmailRequest request) {
        accountSecurity.resendVerification(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody EmailRequest request) {
        accountSecurity.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountSecurity.resetPassword(request.token(), request.password());
        return ResponseEntity.ok(Map.of("reset", true));
    }

    public record TokenRequest(@NotBlank @Size(max = 200) String token) {}
    public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) {}
    public record ResetPasswordRequest(@NotBlank @Size(max = 200) String token,
                                       @NotBlank @Size(min = 8, max = 100, message = "Password must be at least 8 characters") String password) {}

    @PostMapping("/logout") public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) { authService.logout(request.refreshToken()); return ResponseEntity.noContent().build(); }
}
