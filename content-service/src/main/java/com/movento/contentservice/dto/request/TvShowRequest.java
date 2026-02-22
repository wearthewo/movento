package com.movento.contentservice.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonTypeName("TV_SHOW")
public class TvShowRequest extends ContentRequest {
    
    @PositiveOrZero(message = "Number of seasons must be a positive number or zero")
    private Integer numberOfSeasons = 0;
    
    @PositiveOrZero(message = "Number of episodes must be a positive number or zero")
    private Integer numberOfEpisodes = 0;
    
    @NotNull(message = "isOngoing cannot be null")
    private Boolean isOngoing = true;
    
    @Valid
    private List<SeasonRequest> seasons = new ArrayList<>();
    
    @Getter
    @Setter
    public static class SeasonRequest {
        
        @PositiveOrZero(message = "Season number must be a positive number or zero")
        private Integer seasonNumber;
        
        @NotNull(message = "Title cannot be null")
        private String title;
        
        private String description;
        
        private String releaseDate;
        
        private String posterUrl;
        
        @Valid
        private List<EpisodeRequest> episodes = new ArrayList<>();
    }
    
    @Getter
    @Setter
    public static class EpisodeRequest {
        
        @PositiveOrZero(message = "Episode number must be a positive number or zero")
        private Integer episodeNumber;
        
        @NotNull(message = "Title cannot be null")
        private String title;
        
        private String description;
        
        @PositiveOrZero(message = "Duration must be a positive number or zero")
        private Integer durationMinutes;
        
        private String thumbnailUrl;
        
        private String videoUrl;
        
        private String releaseDate;
    }
}
