package com.movento.contentservice.dto.mapper;

import com.movento.contentservice.dto.ContentDto;
import com.movento.contentservice.model.Content;
import com.movento.contentservice.model.ContentRating;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ContentMapper {
    ContentMapper INSTANCE = Mappers.getMapper(ContentMapper.class);
    
    @Mapping(target = "genreIds", expression = "java(content.getGenres().stream().map(genre -> genre.getId()).collect(java.util.stream.Collectors.toList()))")
    @Mapping(target = "userRating", expression = "java(getUserRating(content, userId))")
    ContentDto toDto(Content content, Long userId);
    
    default Integer getUserRating(Content content, Long userId) {
        if (userId == null) return null;
        return content.getRatings().stream()
            .filter(rating -> rating.getUserId().equals(userId))
            .findFirst()
            .map(ContentRating::getRating)
            .orElse(null);
    }
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "genres", ignore = true) // Will be handled in service
    Content toEntity(ContentDto dto);
    
    default ContentDto createDtoInstance(Content content) {
        if (content instanceof com.movento.contentservice.model.Movie) {
            return new com.movento.contentservice.dto.MovieDto();
        } else if (content instanceof com.movento.contentservice.model.TvShow) {
            return new com.movento.contentservice.dto.TvShowDto();
        }
        throw new IllegalArgumentException("Unknown content type: " + content.getClass());
    }
}
