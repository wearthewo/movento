package com.movento.userservice.controller;

import com.movento.userservice.dto.UserProfileDto;
import com.movento.userservice.model.User;
import com.movento.userservice.security.UserDetailsImpl;
import com.movento.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        User user = userService.getUserById(userDetails.getId());
        UserProfileDto profile = new UserProfileDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhoneNumber(),
            user.getProfilePictureUrl()
        );
        
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        UserProfileDto profile = new UserProfileDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhoneNumber(),
            user.getProfilePictureUrl()
        );
        
        return ResponseEntity.ok(profile);
    }
}
