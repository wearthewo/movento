package com.movento.contentservice.dto.mapper;

import com.movento.contentservice.dto.ContentDto;
import com.movento.contentservice.dto.MovieDto;
import com.movento.contentservice.dto.TvShowDto;
import com.movento.contentservice.model.Content;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ContentMapper {
    ContentMapper INSTANCE = Mappers.getMapper(ContentMapper.class);
    
    MovieDto toMovieDto(com.movento.contentservice.model.Movie content, Long userId);
    
    TvShowDto toTvShowDto(com.movento.contentservice.model.TvShow content, Long userId);
    
    default ContentDto toDto(Content content, Long userId) {
        if (content instanceof com.movento.contentservice.model.Movie movie) {
            return toMovieDto(movie, userId);
        } else if (content instanceof com.movento.contentservice.model.TvShow tvShow) {
            return toTvShowDto(tvShow, userId);
        }
        throw new IllegalArgumentException("Unknown content type: " + content.getClass());
    }
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "genres", ignore = true) // Will be handled in service
    Content toEntity(ContentDto dto);
}
