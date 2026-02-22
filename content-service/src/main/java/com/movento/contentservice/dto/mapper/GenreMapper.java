package com.movento.contentservice.dto.mapper;

import com.movento.contentservice.dto.GenreDto;
import com.movento.contentservice.dto.request.GenreRequest;
import com.movento.contentservice.model.Genre;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface GenreMapper {
    
    GenreMapper INSTANCE = Mappers.getMapper(GenreMapper.class);
    
    /**
     * Converts a Genre entity to GenreDto
     * @param genre the Genre entity to convert
     * @return the converted GenreDto
     */
    GenreDto toDto(Genre genre);
    
    /**
     * Converts a GenreRequest to Genre entity
     * @param request the GenreRequest to convert
     * @return the converted Genre entity
     */
    @Mapping(target = "id", ignore = true) // ID is auto-generated
    @Mapping(target = "contents", ignore = true) // Ignore the contents relationship
    Genre toEntity(GenreRequest request);
}
