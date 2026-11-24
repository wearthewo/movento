package com.movento.userservice.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountTokenRepository extends JpaRepository<AccountToken, UUID> {
    Optional<AccountToken> findByTokenHashAndPurpose(String tokenHash, AccountToken.Purpose purpose);

    /** Issuing a new link invalidates older unused links of the same kind. */
    @Modifying
    @Query("update AccountToken t set t.usedAt = :now where t.user.id = :userId and t.purpose = :purpose and t.usedAt is null")
    int invalidateOpen(@Param("userId") Long userId, @Param("purpose") AccountToken.Purpose purpose, @Param("now") Instant now);
}
