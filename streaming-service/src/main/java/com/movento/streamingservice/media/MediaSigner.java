package com.movento.streamingservice.media;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * HMAC-signed, expiring media URLs — the same idea as CDN signed URLs. A leaked link stops working
 * after its expiry and cannot be altered to point at another session or title.
 */
@Component
public class MediaSigner {
    private final byte[] secret;

    public MediaSigner(@Value("${app.media.signing-secret}") String secret) {
        if (secret == null || secret.length() < 16) throw new IllegalStateException("app.media.signing-secret must be at least 16 characters");
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(long sessionId, long assetId, long expiresEpochSeconds) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal((sessionId + ":" + assetId + ":" + expiresEpochSeconds).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public boolean verify(long sessionId, long assetId, long expiresEpochSeconds, String signature) {
        if (signature == null || expiresEpochSeconds < Instant.now().getEpochSecond()) return false;
        byte[] expected = sign(sessionId, assetId, expiresEpochSeconds).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8)); // constant time
    }
}
