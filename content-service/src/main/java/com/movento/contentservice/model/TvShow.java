package com.movento.contentservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString(exclude = "seasons")
@Entity
@Table(name = "tv_shows")
@PrimaryKeyJoinColumn(name = "id")
public class TvShow extends Content {
    
    @Column(name = "number_of_seasons")
    private Integer numberOfSeasons = 1;
    
    @Column(name = "number_of_episodes")
    private Integer numberOfEpisodes = 0;
    
    @Column(name = "is_ongoing")
    private Boolean isOngoing = true;
    
    @OneToMany(mappedBy = "tvShow", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Season> seasons = new HashSet<>();
    
    // Helper methods
    public void addSeason(Season season) {
        this.seasons.add(season);
        season.setTvShow(this);
        this.numberOfSeasons = this.seasons.size();
    }
    
    public void removeSeason(Season season) {
        this.seasons.remove(season);
        season.setTvShow(null);
        this.numberOfSeasons = this.seasons.size();
    }
    
    public void updateEpisodeCount() {
        this.numberOfEpisodes = this.seasons.stream()
            .mapToInt(season -> season.getEpisodes().size())
            .sum();
    }
}
