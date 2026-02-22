package com.movento.contentservice.service;

import com.movento.contentservice.model.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import java.io.Serializable;

public interface BaseService<T extends BaseEntity, ID extends Serializable> {
    List<T> findAll();
    Page<T> findAll(Pageable pageable);
    Optional<T> findById(ID id);
    T save(T entity);
    T update(ID id, T entity);
    void deleteById(ID id);
    boolean existsById(ID id);
}
