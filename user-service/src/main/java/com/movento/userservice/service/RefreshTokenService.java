package com.movento.userservice.service;
import com.movento.userservice.model.RefreshToken;
import com.movento.userservice.model.User;
import com.movento.userservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class RefreshTokenService {
 private final RefreshTokenRepository tokens;
 @Value("${spring.security.jwt.refresh-expiration-ms:2592000000}") private long lifetime;
 @Transactional public String issue(User user) { byte[] bytes=new byte[48]; new SecureRandom().nextBytes(bytes); String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); RefreshToken token=new RefreshToken(); token.setId(UUID.randomUUID()); token.setUser(user); token.setTokenHash(hash(raw)); token.setExpiresAt(Instant.now().plusMillis(lifetime)); tokens.save(token); return raw; }
 @Transactional public Rotation rotate(String raw) { RefreshToken current=tokens.findByTokenHash(hash(raw)).orElseThrow(); if(current.getRevokedAt()!=null || current.getExpiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("Refresh token expired"); current.setRevokedAt(Instant.now()); return new Rotation(current.getUser(), issue(current.getUser())); }
 @Transactional public void revoke(String raw) { tokens.findByTokenHash(hash(raw)).ifPresent(t -> t.setRevokedAt(Instant.now())); }
 private String hash(String raw) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); } catch(Exception e) { throw new IllegalStateException(e); } }
 public record Rotation(User user, String token) {}
}
