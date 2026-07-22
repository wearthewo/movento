package com.movento.userservice.repository;

import com.movento.userservice.model.ViewerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ViewerProfileRepository extends JpaRepository<ViewerProfile, UUID> {
    List<ViewerProfile> findByUserIdOrderByCreatedAt(Long userId);
    long countByUserId(Long userId);
    Optional<ViewerProfile> findByIdAndUserId(UUID id, Long userId);
}
