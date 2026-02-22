package com.movento.contentservice.service;

import com.movento.contentservice.model.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GenreService extends BaseService<Genre, Long> {
    Page<Genre> findByNameContaining(String name, Pageable pageable);
}
