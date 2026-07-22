package com.movento.userservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "viewer_profiles") @Getter @Setter @NoArgsConstructor
public class ViewerProfile {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false, length = 50) private String name;
    @Column(name = "avatar_url", length = 512) private String avatarUrl;
    @Column(name = "kids_mode", nullable = false) private boolean kidsMode;
    @Column(name = "maturity_level", nullable = false, length = 20) private String maturityLevel = "ADULT";
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
