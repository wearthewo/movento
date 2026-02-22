package com.movento.contentservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SeasonDto extends BaseDto {
    private Integer seasonNumber;
    private String title;
    private String description;
    private LocalDate releaseDate;
    private String posterUrl;
    private List<EpisodeDto> episodes = new ArrayList<>();
}
