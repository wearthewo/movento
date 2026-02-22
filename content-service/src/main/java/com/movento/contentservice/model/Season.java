package com.movento.contentservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString(exclude = {"tvShow", "episodes"})
@Entity
@Table(name = "seasons",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tv_show_id", "season_number"}))
public class Season extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tv_show_id", nullable = false)
    private TvShow tvShow;
    
    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "release_date")
    private LocalDate releaseDate;
    
    @Column(name = "poster_url", length = 512)
    private String posterUrl;
    
    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Episode> episodes = new HashSet<>();
    
    // Helper methods
    public void addEpisode(Episode episode) {
        this.episodes.add(episode);
        episode.setSeason(this);
        this.tvShow.updateEpisodeCount();
    }
    
    public void removeEpisode(Episode episode) {
        this.episodes.remove(episode);
        episode.setSeason(null);
        this.tvShow.updateEpisodeCount();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Season)) return false;
        return getId() != null && getId().equals(((Season) o).getId());
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
