package com.movento.userservice.service;

import com.movento.userservice.dto.JwtResponse;
import com.movento.userservice.dto.LoginRequest;
import com.movento.userservice.dto.SignupRequest;
import com.movento.userservice.exception.EmailAlreadyExistsException;
import com.movento.userservice.model.ERole;
import com.movento.userservice.model.Role;
import com.movento.userservice.model.User;
import com.movento.userservice.repository.RoleRepository;
import com.movento.userservice.repository.UserRepository;
import com.movento.userservice.repository.ViewerProfileRepository;
import com.movento.userservice.model.ViewerProfile;
import com.movento.userservice.config.JwtUtils;
import com.movento.userservice.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RefreshTokenService refreshTokens;

    @Autowired
    private ViewerProfileRepository viewerProfiles;

    @Value("${app.admin-email:}")
    private String adminEmail;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        JwtResponse response = new JwtResponse(jwt,
                             userDetails.getId(), 
                             userDetails.getEmail(), 
                             roles);
        response.setRefreshToken(refreshTokens.issue(userRepository.findById(userDetails.getId()).orElseThrow()));
        return response;
    }

    public JwtResponse refresh(String rawToken) {
        RefreshTokenService.Rotation rotation = refreshTokens.rotate(rawToken);
        UserDetailsImpl details = UserDetailsImpl.build(rotation.user());
        JwtResponse response = new JwtResponse(jwtUtils.generateToken(details), details.getId(), details.getEmail(), details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        response.setRefreshToken(rotation.token()); return response;
    }

    public void logout(String rawToken) { refreshTokens.revoke(rawToken); }

    public JwtResponse registerUser(SignupRequest signUpRequest) {
        String normalizedEmail = signUpRequest.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email is already in use!");
        }

        // Create new user's account
        User user = new User(
            normalizedEmail,
            encoder.encode(signUpRequest.getPassword()),
            signUpRequest.getFirstName(),
            signUpRequest.getLastName()
        );

        Set<Role> roles = new HashSet<>();
        
        if (!adminEmail.isBlank() && normalizedEmail.equalsIgnoreCase(adminEmail)) {
            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(adminRole);
        } else {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        }

        user.setRoles(roles);
        user = userRepository.save(user);
        ViewerProfile profile = new ViewerProfile();
        profile.setUser(user); profile.setName(signUpRequest.getFirstName()); profile.setMaturityLevel("ADULT");
        viewerProfiles.save(profile);
        return authenticateUser(new LoginRequest(normalizedEmail, signUpRequest.getPassword()));
    }
}
