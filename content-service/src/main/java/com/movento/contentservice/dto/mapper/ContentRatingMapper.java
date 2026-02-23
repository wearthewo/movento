package com.movento.contentservice.dto.mapper;

import com.movento.contentservice.dto.ContentRatingDto;
import com.movento.contentservice.dto.request.ContentRatingRequest;
import com.movento.contentservice.model.ContentRating;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ContentRatingMapper {
    
    ContentRatingMapper INSTANCE = Mappers.getMapper(ContentRatingMapper.class);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ContentRating toEntity(ContentRatingRequest request);
    
    ContentRatingDto toDto(ContentRating contentRating);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ContentRatingRequest request, @MappingTarget ContentRating entity);
}
