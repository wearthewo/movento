package com.movento.contentservice.controller;

import com.movento.contentservice.dto.ContentDto;
import com.movento.contentservice.dto.request.ContentRequest;
import com.movento.contentservice.dto.request.MovieRequest;
import com.movento.contentservice.dto.request.TvShowRequest;
import com.movento.contentservice.dto.response.ApiResponse;
import com.movento.contentservice.model.Content;
import com.movento.contentservice.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ContentDto>>> getAllContents(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long genreId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<ContentDto> contents;
        if (title != null) {
            contents = contentService.searchByTitle(title, pageable)
                .map(content -> contentService.getContentDetails(content.getId(), null));
        } else if (genreId != null) {
            contents = contentService.findByGenreId(genreId, pageable)
                .map(content -> contentService.getContentDetails(content.getId(), null));
        } else {
            contents = contentService.findAll(pageable)
                .map(content -> contentService.getContentDetails(content.getId(), null));
        }
        
        return ResponseEntity.ok(ApiResponse.success(contents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentDto>> getContentById(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        
        ContentDto content = contentService.getContentDetails(id, userId);
        return ResponseEntity.ok(ApiResponse.success(content));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContentDto>> createContent(
            @Valid @RequestBody ContentRequest request) {
        
        Content createdContent;
        List<Long> genreIds = request.getGenres().stream()
                .map(genreDto -> genreDto.getId())
                .toList();
        
        if (request instanceof MovieRequest movieRequest) {
            createdContent = contentService.createMovie(movieRequest, genreIds);
        } else if (request instanceof TvShowRequest tvShowRequest) {
            createdContent = contentService.createTvShow(tvShowRequest, genreIds);
        } else {
            throw new IllegalArgumentException("Unsupported content type");
        }
        
        ContentDto contentDto = contentService.getContentDetails(createdContent.getId(), null);
        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(createdContent.getId())
            .toUri();
            
        return ResponseEntity.created(location)
            .body(ApiResponse.created(contentDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentDto>> updateContent(
            @PathVariable Long id,
            @Valid @RequestBody ContentRequest request) {
        
        Content updatedContent;
        List<Long> genreIds = request.getGenres().stream()
                .map(genreDto -> genreDto.getId())
                .toList();
        
        if (request instanceof MovieRequest movieRequest) {
            updatedContent = contentService.updateMovie(id, movieRequest, genreIds);
        } else if (request instanceof TvShowRequest tvShowRequest) {
            updatedContent = contentService.updateTvShow(id, tvShowRequest, genreIds);
        } else {
            throw new IllegalArgumentException("Unsupported content type");
        }
        
        ContentDto contentDto = contentService.getContentDetails(updatedContent.getId(), null);
        return ResponseEntity.ok(ApiResponse.success(contentDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable Long id) {
        contentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/top-rated")
    public ResponseEntity<ApiResponse<Page<ContentDto>>> getTopRated(
            @PageableDefault(size = 10) Pageable pageable) {
        
        Page<ContentDto> contents = contentService.findTopRated(pageable)
            .map(content -> contentService.getContentDetails(content.getId(), null));
            
        return ResponseEntity.ok(ApiResponse.success(contents));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<Page<ContentDto>>> getRecentlyAdded(
            @PageableDefault(size = 10) Pageable pageable) {
        
        Page<ContentDto> contents = contentService.findRecentlyAdded(pageable)
            .map(content -> contentService.getContentDetails(content.getId(), null));
            
        return ResponseEntity.ok(ApiResponse.success(contents));
    }
}
