package com.movento.contentservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Entity
@Table(name = "episodes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"season_id", "episode_number"}))
public class Episode extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;
    
    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;
    
    @Column(name = "video_url", length = 512)
    private String videoUrl;
    
    @Column(name = "release_date")
    private LocalDateTime releaseDate;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Episode)) return false;
        return getId() != null && getId().equals(((Episode) o).getId());
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
