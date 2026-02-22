package com.movento.contentservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TvShowDto extends ContentDto {
    private Integer numberOfSeasons;
    private Integer numberOfEpisodes;
    private Boolean isOngoing;
    private List<SeasonDto> seasons = new ArrayList<>();
}
