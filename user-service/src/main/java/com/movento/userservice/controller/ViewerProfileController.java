package com.movento.userservice.controller;

import com.movento.userservice.dto.ViewerProfileRequest;
import com.movento.userservice.dto.ViewerProfileResponse;
import com.movento.userservice.model.ViewerProfile;
import com.movento.userservice.repository.UserRepository;
import com.movento.userservice.repository.ViewerProfileRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import com.movento.userservice.config.JwtUtils;
import com.movento.userservice.security.UserDetailsImpl;
import com.movento.userservice.exception.ProfileRuleException;

@RestController @RequestMapping("/api/v1/users/me/profiles") @RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class ViewerProfileController {
    private static final int MAX_PROFILES = 5;
    private final ViewerProfileRepository profiles;
    private final UserRepository users;
    private final JwtUtils jwtUtils;

    @GetMapping
    public List<ViewerProfileResponse> list(@RequestHeader("X-Account-Id") Long accountId) {
        return profiles.findByUserIdOrderByCreatedAt(accountId).stream().map(this::response).toList();
    }

    @PostMapping @Transactional
    public ResponseEntity<ViewerProfileResponse> create(@RequestHeader("X-Account-Id") Long accountId, @Valid @RequestBody ViewerProfileRequest request) {
        if (profiles.countByUserId(accountId) >= MAX_PROFILES) throw new ProfileRuleException("An account can have up to " + MAX_PROFILES + " profiles.");
        ViewerProfile profile = new ViewerProfile(); profile.setUser(users.findById(accountId).orElseThrow()); apply(profile, request);
        profile = profiles.save(profile);
        log.info("Profile created account={} profile={} kids={}", accountId, profile.getId(), profile.isKidsMode());
        return ResponseEntity.created(URI.create("/api/v1/users/me/profiles/" + profile.getId())).body(response(profile));
    }

    @PutMapping("/{id}") @Transactional
    public ViewerProfileResponse update(@RequestHeader("X-Account-Id") Long accountId, @PathVariable UUID id, @Valid @RequestBody ViewerProfileRequest request) {
        ViewerProfile profile = profiles.findByIdAndUserId(id, accountId).orElseThrow(); apply(profile, request); return response(profile);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-Account-Id") Long accountId, @PathVariable UUID id) {
        if (profiles.countByUserId(accountId) <= 1) throw new ProfileRuleException("An account needs at least one profile.");
        ViewerProfile profile = profiles.findByIdAndUserId(id, accountId).orElseThrow(); profiles.delete(profile);
        log.info("Profile deleted account={} profile={}", accountId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/select") @Transactional(readOnly = true)
    public Map<String, Object> select(@RequestHeader("X-Account-Id") Long accountId, @PathVariable UUID id) {
        ViewerProfile profile = profiles.findByIdAndUserId(id, accountId).orElseThrow();
        UserDetailsImpl details = UserDetailsImpl.build(users.findById(accountId).orElseThrow());
        log.info("Profile selected account={} profile={} maturity={}", accountId, profile.getId(), profile.isKidsMode() ? "KIDS" : profile.getMaturityLevel());
        return Map.of("accessToken", jwtUtils.generateToken(details, profile), "expiresIn", jwtUtils.getExpirationSeconds(),
                "profile", response(profile));
    }

    private void apply(ViewerProfile p, ViewerProfileRequest r) { p.setName(r.name().trim()); p.setAvatarUrl(r.avatarUrl()); p.setKidsMode(r.kidsMode()); p.setMaturityLevel(r.kidsMode() ? "KIDS" : (r.maturityLevel() == null ? "ADULT" : r.maturityLevel())); }
    private ViewerProfileResponse response(ViewerProfile p) { return new ViewerProfileResponse(p.getId(), p.getName(), p.getAvatarUrl(), p.isKidsMode(), p.getMaturityLevel()); }
}
