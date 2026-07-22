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

@RestController @RequestMapping("/api/v1/users/me/profiles") @RequiredArgsConstructor
public class ViewerProfileController {
    private final ViewerProfileRepository profiles;
    private final UserRepository users;
    private final JwtUtils jwtUtils;

    @GetMapping
    public List<ViewerProfileResponse> list(@RequestHeader("X-Account-Id") Long accountId) {
        return profiles.findByUserIdOrderByCreatedAt(accountId).stream().map(this::response).toList();
    }

    @PostMapping @Transactional
    public ResponseEntity<ViewerProfileResponse> create(@RequestHeader("X-Account-Id") Long accountId, @Valid @RequestBody ViewerProfileRequest request) {
        if (profiles.countByUserId(accountId) >= 5) return ResponseEntity.unprocessableEntity().build();
        ViewerProfile profile = new ViewerProfile(); profile.setUser(users.findById(accountId).orElseThrow()); apply(profile, request);
        profile = profiles.save(profile);
        return ResponseEntity.created(URI.create("/api/v1/users/me/profiles/" + profile.getId())).body(response(profile));
    }

    @PutMapping("/{id}") @Transactional
    public ViewerProfileResponse update(@RequestHeader("X-Account-Id") Long accountId, @PathVariable UUID id, @Valid @RequestBody ViewerProfileRequest request) {
        ViewerProfile profile = profiles.findByIdAndUserId(id, accountId).orElseThrow(); apply(profile, request); return response(profile);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-Account-Id") Long accountId, @PathVariable UUID id) {
        ViewerProfile profile = profiles.findByIdAndUserId(id, accountId).orElseThrow(); profiles.delete(profile); return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/select")
    public Map<String, Object> select(@RequestHeader("X-Account-Id") Long accountId, @PathVariable UUID id) {
        profiles.findByIdAndUserId(id, accountId).orElseThrow();
        UserDetailsImpl details = UserDetailsImpl.build(users.findById(accountId).orElseThrow());
        return Map.of("accessToken", jwtUtils.generateToken(details, id), "expiresIn", 900);
    }

    private void apply(ViewerProfile p, ViewerProfileRequest r) { p.setName(r.name().trim()); p.setAvatarUrl(r.avatarUrl()); p.setKidsMode(r.kidsMode()); p.setMaturityLevel(r.kidsMode() ? "KIDS" : (r.maturityLevel() == null ? "ADULT" : r.maturityLevel())); }
    private ViewerProfileResponse response(ViewerProfile p) { return new ViewerProfileResponse(p.getId(), p.getName(), p.getAvatarUrl(), p.isKidsMode(), p.getMaturityLevel()); }
}
