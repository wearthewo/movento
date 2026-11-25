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
@lombok.extern.slf4j.Slf4j
public class RefreshTokenService {
 private final RefreshTokenRepository tokens;
 @Value("${spring.security.jwt.refresh-expiration-ms:2592000000}") private long lifetime;
 @Transactional public String issue(User user) { byte[] bytes=new byte[48]; RANDOM.nextBytes(bytes); String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); RefreshToken token=new RefreshToken(); token.setId(UUID.randomUUID()); token.setUser(user); token.setTokenHash(hash(raw)); token.setExpiresAt(Instant.now().plusMillis(lifetime)); tokens.save(token); return raw; }
 /**
  * Rotation with reuse detection: presenting an already-rotated token means it was copied, so every
  * session of that account is revoked (noRollbackFor keeps the revocation when we throw).
  */
 @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
 public Rotation rotate(String raw) {
  RefreshToken current=tokens.findByTokenHash(hash(raw)).orElseThrow(InvalidRefreshTokenException::new);
  if(current.getRevokedAt()!=null) {
   // Parallel requests from the same browser can present the same token a moment apart: allow a short grace window.
   if(current.getRevokedAt().isAfter(Instant.now().minus(REUSE_GRACE))) {
    log.debug("Refresh token reused within grace window (parallel request) account={}", current.getUser().getId());
    return new Rotation(current.getUser(), issue(current.getUser()));
   }
   int revoked = tokens.revokeAllForUser(current.getUser().getId(), Instant.now());
   log.warn("Refresh token REUSE detected account={}: possible theft, revoked {} active session(s)", current.getUser().getId(), revoked);
   throw new InvalidRefreshTokenException();
  }
  if(current.getExpiresAt().isBefore(Instant.now())) throw new InvalidRefreshTokenException(); current.setRevokedAt(Instant.now()); return new Rotation(current.getUser(), issue(current.getUser())); }
 @Transactional public void revoke(String raw) { tokens.findByTokenHash(hash(raw)).ifPresent(t -> t.setRevokedAt(Instant.now())); }
 private String hash(String raw) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); } catch(Exception e) { throw new IllegalStateException(e); } }
 private static final SecureRandom RANDOM = new SecureRandom();
 private static final java.time.Duration REUSE_GRACE = java.time.Duration.ofSeconds(30);
 public record Rotation(User user, String token) {}
 /** Mapped to 401 so the web app clears its session instead of surfacing a 500. */
 @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
 public static class InvalidRefreshTokenException extends RuntimeException { public InvalidRefreshTokenException() { super("Your session has expired. Please sign in again."); } }
}
