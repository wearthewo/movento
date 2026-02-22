package com.movento.userservice.dto;

import lombok.Data;

@Data
public class UserProfileDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profilePictureUrl;

    public UserProfileDto(Long id, String email, String firstName, String lastName, 
                         String phoneNumber, String profilePictureUrl) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.profilePictureUrl = profilePictureUrl;
    }
}
