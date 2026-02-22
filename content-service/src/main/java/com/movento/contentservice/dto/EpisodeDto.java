package com.movento.contentservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EpisodeDto extends BaseDto {
    private Integer episodeNumber;
    private String title;
    private String description;
    private Integer durationMinutes;
    private String thumbnailUrl;
    private String videoUrl;
    private LocalDateTime releaseDate;
    private ViewHistoryDto viewHistory;
}
