package com.movento.contentservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ViewHistoryDto extends BaseDto {
    private Long contentId;
    private Long userId;
    private Long episodeId;
    private Integer progressSeconds;
    private boolean completed;
    private LocalDateTime lastWatchedAt;
}
