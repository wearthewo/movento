package com.movento.contentservice.dto.mapper;

import com.movento.contentservice.dto.ViewHistoryDto;
import com.movento.contentservice.dto.request.ViewHistoryRequest;
import com.movento.contentservice.model.ViewHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ViewHistoryMapper {
    
    ViewHistoryMapper INSTANCE = Mappers.getMapper(ViewHistoryMapper.class);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "viewedAt", expression = "java(java.time.LocalDateTime.now())")
    ViewHistory toEntity(ViewHistoryRequest request);
    
    ViewHistoryDto toDto(ViewHistory viewHistory);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "viewedAt", expression = "java(java.time.LocalDateTime.now())")
    void updateEntityFromRequest(ViewHistoryRequest request, @MappingTarget ViewHistory entity);
}
