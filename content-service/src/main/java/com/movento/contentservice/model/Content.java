package com.movento.contentservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import javax.persistence.DiscriminatorColumn;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString(exclude = {"genres", "ratings", "viewHistories"})
@Entity
@Table(name = "content")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "content_type", discriminatorType = DiscriminatorType.STRING)
public class Content extends BaseEntity {
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "release_year")
    private Integer releaseYear;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;
    
    @Column(name = "backdrop_url", length = 512)
    private String backdropUrl;
    
    @Column(name = "content_rating", length = 10)
    private String contentRating;
    
    @Column(name = "is_active")
    private boolean active = true;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "content_genres",
        joinColumns = @JoinColumn(name = "content_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();
    
    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ContentRating> ratings = new HashSet<>();
    
    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ViewHistory> viewHistories = new HashSet<>();
    
    // Helper methods
    public void addGenre(Genre genre) {
        this.genres.add(genre);
        genre.getContents().add(this);
    }
    
    public void removeGenre(Genre genre) {
        this.genres.remove(genre);
        genre.getContents().remove(this);
    }
    
    public void addRating(ContentRating rating) {
        this.ratings.add(rating);
        rating.setContent(this);
    }
    
    public void removeRating(ContentRating rating) {
        this.ratings.remove(rating);
        rating.setContent(null);
    }
}
