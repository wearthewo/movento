package com.movento.contentservice.service;

import com.movento.contentservice.exceptions.ResourceNotFoundException;
import com.movento.contentservice.model.Genre;
import com.movento.contentservice.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GenreServiceImpl extends BaseServiceImpl<Genre, Long> implements GenreService {

    private final GenreRepository genreRepository;

    public GenreServiceImpl(GenreRepository genreRepository) {
        super(genreRepository);
        this.genreRepository = genreRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Genre> findByNameContaining(String name, Pageable pageable) {
        return genreRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    @Override
    @Transactional
    public Genre update(Long id, Genre genre) {
        return genreRepository.findById(id).map(existingGenre -> {
            existingGenre.setName(genre.getName());
            existingGenre.setDescription(genre.getDescription());
            return genreRepository.save(existingGenre);
        }).orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));
    }
}
