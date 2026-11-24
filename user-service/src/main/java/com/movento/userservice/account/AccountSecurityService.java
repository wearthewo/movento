package com.movento.userservice.account;

import com.movento.userservice.model.User;
import com.movento.userservice.repository.RefreshTokenRepository;
import com.movento.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Brute-force lockout, email verification and password reset.
 * Responses never reveal whether an email is registered (forgot-password / resend always succeed).
 */
@Service
@RequiredArgsConstructor
public class AccountSecurityService {
    private static final Duration VERIFY_TTL = Duration.ofHours(24);
    private static final Duration RESET_TTL = Duration.ofMinutes(30);

    private final UserRepository users;
    private final AccountTokenRepository tokens;
    private final RefreshTokenRepository refreshTokens;
    private final AccountMailer mailer;
    private final PasswordEncoder encoder;

    @Value("${app.security.max-failed-logins:5}")
    private int maxFailedLogins;

    @Value("${app.security.lockout-minutes:15}")
    private long lockoutMinutes;

    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public static class AccountLockedException extends RuntimeException {
        public AccountLockedException(long minutes) {
            super("Too many failed sign-in attempts. Try again in " + Math.max(1, minutes) + " minute" + (minutes == 1 ? "" : "s") + ".");
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidAccountTokenException extends RuntimeException {
        public InvalidAccountTokenException() {
            super("This link is invalid or has expired. Please request a new one.");
        }
    }

    // ---- lockout -------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public void ensureNotLocked(String email) {
        users.findByEmail(email).ifPresent(user -> {
            Instant until = user.getLockedUntil();
            if (until != null && until.isAfter(Instant.now())) {
                throw new AccountLockedException(Duration.between(Instant.now(), until).toMinutes() + 1);
            }
        });
    }

    /** Own transaction so the counter survives the failed authentication. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedLogin(String email) {
        users.findByEmail(email).ifPresent(user -> {
            int failures = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failures);
            if (failures >= maxFailedLogins) {
                user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(lockoutMinutes)));
                user.setFailedLoginAttempts(0);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccessfulLogin(String email) {
        users.findByEmail(email).ifPresent(user -> {
            if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
            }
        });
    }

    // ---- email verification --------------------------------------------------------------------

    @Transactional
    public void sendVerification(User user) {
        if (user.isEmailVerified()) return;
        mailer.sendVerification(user.getEmail(), user.getFirstName(), issue(user, AccountToken.Purpose.VERIFY_EMAIL, VERIFY_TTL));
    }

    @Transactional
    public void resendVerification(String email) {
        users.findByEmail(normalize(email)).ifPresent(this::sendVerification);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        User user = consume(rawToken, AccountToken.Purpose.VERIFY_EMAIL);
        user.setEmailVerified(true);
    }

    // ---- password reset ------------------------------------------------------------------------

    @Transactional
    public void requestPasswordReset(String email) {
        users.findByEmail(normalize(email)).ifPresent(user ->
                mailer.sendPasswordReset(user.getEmail(), user.getFirstName(), issue(user, AccountToken.Purpose.RESET_PASSWORD, RESET_TTL)));
    }

    /** Sets the new password, clears any lockout and signs the account out of every device. */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        User user = consume(rawToken, AccountToken.Purpose.RESET_PASSWORD);
        user.setPassword(encoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setEmailVerified(true); // they proved they own the inbox
        refreshTokens.revokeAllForUser(user.getId(), Instant.now());
    }

    // ---- tokens --------------------------------------------------------------------------------

    private String issue(User user, AccountToken.Purpose purpose, Duration ttl) {
        tokens.invalidateOpen(user.getId(), purpose, Instant.now());
        String raw = TokenCodec.newToken();
        AccountToken token = new AccountToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setPurpose(purpose);
        token.setTokenHash(TokenCodec.hash(raw));
        token.setExpiresAt(Instant.now().plus(ttl));
        tokens.save(token);
        return raw;
    }

    private User consume(String rawToken, AccountToken.Purpose purpose) {
        AccountToken token = tokens.findByTokenHashAndPurpose(TokenCodec.hash(rawToken), purpose)
                .orElseThrow(InvalidAccountTokenException::new);
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) throw new InvalidAccountTokenException();
        token.setUsedAt(Instant.now());
        return token.getUser();
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
