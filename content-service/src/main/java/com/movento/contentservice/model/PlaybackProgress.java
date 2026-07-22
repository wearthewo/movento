package com.movento.contentservice.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="playback_progress", uniqueConstraints=@UniqueConstraint(columnNames={"profile_id","content_id","episode_key"})) @Getter @Setter
public class PlaybackProgress { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="profile_id",nullable=false) private UUID profileId; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="content_id") private Content content; @Column(name="episode_key",nullable=false) private String episodeKey=""; @Column(name="progress_seconds",nullable=false) private int progressSeconds; @Column(name="duration_seconds",nullable=false) private int durationSeconds; @Column(nullable=false) private boolean completed; @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now(); }
