package com.movento.userservice.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="refresh_tokens") @Getter @Setter
public class RefreshToken {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id") private User user;
 @Column(name="token_hash", nullable=false, unique=true, length=64) private String tokenHash;
 @Column(name="expires_at", nullable=false) private Instant expiresAt;
 @Column(name="revoked_at") private Instant revokedAt;
 @Column(name="created_at", nullable=false) private Instant createdAt = Instant.now();
}
