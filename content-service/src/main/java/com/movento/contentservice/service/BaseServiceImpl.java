package com.movento.contentservice.service;

import com.movento.contentservice.exceptions.ResourceNotFoundException;
import com.movento.contentservice.model.BaseEntity;
import com.movento.contentservice.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import java.io.Serializable;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseServiceImpl<T extends BaseEntity, ID extends Serializable> implements BaseService<T, ID> {
    
    protected final BaseRepository<T, ID> repository;

    @Override
    @Transactional(readOnly = true)
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(ID id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public T save(T entity) {
        return repository.save(entity);
    }

    @Override
    @Transactional
    public T update(ID id, T entity) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found with id: " + id);
        }
        entity.setId((Long) id);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public void deleteById(ID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found with id: " + id);
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(ID id) {
        return repository.existsById(id);
    }
}
