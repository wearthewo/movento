package com.movento.contentservice.controller;

import com.movento.contentservice.dto.GenreDto;
import com.movento.contentservice.dto.mapper.GenreMapper;
import com.movento.contentservice.dto.request.GenreRequest;
import com.movento.contentservice.dto.response.ApiResponse;
import com.movento.contentservice.model.Genre;
import com.movento.contentservice.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.movento.contentservice.exception.ResourceNotFoundException;

import javax.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;
    private final GenreMapper genreMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<GenreDto>>> getAllGenres(Pageable pageable) {
        Page<GenreDto> genres = genreService.findAll(pageable)
            .map(genreMapper::toDto);
        return ResponseEntity.ok(ApiResponse.success(genres));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GenreDto>> getGenreById(@PathVariable Long id) {
        GenreDto genre = genreService.findById(id)
            .map(genreMapper::toDto)
            .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));
        return ResponseEntity.ok(ApiResponse.success(genre));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GenreDto>> createGenre(
            @Valid @RequestBody GenreRequest request) {
        
        Genre genre = genreMapper.toEntity(request);
        Genre createdGenre = genreService.save(genre);
        GenreDto genreDto = genreMapper.toDto(createdGenre);
        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(createdGenre.getId())
            .toUri();
            
        return ResponseEntity.created(location)
            .body(ApiResponse.created(genreDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GenreDto>> updateGenre(
            @PathVariable Long id,
            @Valid @RequestBody GenreRequest request) {
        
        Genre genre = genreService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));
            
        genre.setName(request.getName());
        genre.setDescription(request.getDescription());
        
        Genre updatedGenre = genreService.save(genre);
        return ResponseEntity.ok(ApiResponse.success(genreMapper.toDto(updatedGenre)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGenre(@PathVariable Long id) {
        genreService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<GenreDto>>> searchGenres(
            @RequestParam String name,
            Pageable pageable) {
        
        Page<GenreDto> genres = genreService.findByNameContaining(name, pageable)
            .map(genreMapper::toDto);
            
        return ResponseEntity.ok(ApiResponse.success(genres));
    }
}
