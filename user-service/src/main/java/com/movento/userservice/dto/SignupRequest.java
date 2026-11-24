package com.movento.userservice.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class SignupRequest {
    @NotBlank
    @Size(min = 1, max = 50)
    private String firstName;

    @NotBlank
    @Size(min = 1, max = 50)
    private String lastName;

    @NotBlank
    @Size(max = 254)
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;
}
