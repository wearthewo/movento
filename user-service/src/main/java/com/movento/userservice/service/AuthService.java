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
import com.movento.userservice.account.AccountSecurityService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@lombok.extern.slf4j.Slf4j
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

    @Autowired
    private AccountSecurityService accountSecurity;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        String email = loginRequest.getEmail().trim().toLowerCase();
        accountSecurity.ensureNotLocked(email);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword()));
        } catch (BadCredentialsException badCredentials) {
            accountSecurity.recordFailedLogin(email); // logs the attempt (without the email)
            throw badCredentials;
        }
        accountSecurity.recordSuccessfulLogin(email);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        log.info("Signed in account={}", userDetails.getId());
        String jwt = jwtUtils.generateToken(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        JwtResponse response = new JwtResponse(jwt,
                             userDetails.getId(), 
                             userDetails.getEmail(), 
                             roles);
        response.setExpiresIn(jwtUtils.getExpirationSeconds());
        response.setRefreshToken(refreshTokens.issue(userRepository.findById(userDetails.getId()).orElseThrow()));
        return response;
    }

    /**
     * Rotates the refresh token. When a profileId is supplied (and belongs to the account) the new
     * access token keeps the viewer-profile scope, so a silent refresh does not drop the selected profile.
     */
    @Transactional(noRollbackFor = RefreshTokenService.InvalidRefreshTokenException.class)
    public JwtResponse refresh(String rawToken, java.util.UUID profileId) {
        RefreshTokenService.Rotation rotation = refreshTokens.rotate(rawToken);
        UserDetailsImpl details = UserDetailsImpl.build(rotation.user());
        ViewerProfile profile = profileId == null ? null
            : viewerProfiles.findByIdAndUserId(profileId, details.getId()).orElse(null);
        JwtResponse response = new JwtResponse(jwtUtils.generateToken(details, profile), details.getId(), details.getEmail(), details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        response.setExpiresIn(jwtUtils.getExpirationSeconds());
        response.setRefreshToken(rotation.token());
        log.info("Session refreshed account={} profile={}", details.getId(), profile == null ? "-" : profile.getId());
        return response;
    }

    public void logout(String rawToken) {
        refreshTokens.revoke(rawToken);
        log.info("Signed out: refresh token revoked");
    }

    @Transactional
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

        // Public sign-up always creates a regular viewer; the admin account is bootstrapped from env (AdminBootstrap).
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName(ERole.ROLE_USER)
            .orElseThrow(() -> new IllegalStateException("ROLE_USER is missing")));
        user.setRoles(roles);
        user = userRepository.save(user);
        ViewerProfile profile = new ViewerProfile();
        profile.setUser(user); profile.setName(signUpRequest.getFirstName().trim()); profile.setMaturityLevel("ADULT");
        viewerProfiles.save(profile);
        accountSecurity.sendVerification(user);
        log.info("Account created account={} profile={}", user.getId(), profile.getId());
        return authenticateUser(new LoginRequest(normalizedEmail, signUpRequest.getPassword()));
    }
}
