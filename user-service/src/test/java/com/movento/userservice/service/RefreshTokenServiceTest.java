package com.movento.userservice.service;

import com.movento.userservice.model.RefreshToken;
import com.movento.userservice.model.User;
import com.movento.userservice.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    @Mock RefreshTokenRepository repository;

    @Test void storesOnlyAHashOfTheRefreshToken() {
        RefreshTokenService service = new RefreshTokenService(repository);
        ReflectionTestUtils.setField(service, "lifetime", 60_000L);
        User user = new User("viewer@example.com", "hash", "Movie", "Fan"); user.setId(42L);
        String raw = service.issue(user);
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(raw).isNotBlank();
        assertThat(saved.getValue().getTokenHash()).hasSize(64).doesNotContain(raw);
        assertThat(saved.getValue().getUser()).isSameAs(user);
    }

    @Test void reusingALongRotatedTokenRevokesEverySessionOfTheAccount() {
        RefreshTokenService service = new RefreshTokenService(repository);
        User user = new User("viewer@example.com", "hash", "Movie", "Fan"); user.setId(42L);
        RefreshToken stolen = new RefreshToken();
        stolen.setUser(user);
        stolen.setExpiresAt(Instant.now().plusSeconds(3600));
        stolen.setRevokedAt(Instant.now().minusSeconds(600));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stolen));

        assertThatThrownBy(() -> service.rotate("stolen-token")).isInstanceOf(RefreshTokenService.InvalidRefreshTokenException.class);
        verify(repository).revokeAllForUser(eq(42L), any());
    }

    @Test void parallelRefreshWithinGraceWindowIsNotTreatedAsTheft() {
        RefreshTokenService service = new RefreshTokenService(repository);
        ReflectionTestUtils.setField(service, "lifetime", 60_000L);
        User user = new User("viewer@example.com", "hash", "Movie", "Fan"); user.setId(42L);
        RefreshToken justRotated = new RefreshToken();
        justRotated.setUser(user);
        justRotated.setExpiresAt(Instant.now().plusSeconds(3600));
        justRotated.setRevokedAt(Instant.now().minusSeconds(2));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(justRotated));

        assertThat(service.rotate("same-token").token()).isNotBlank();
        verify(repository, never()).revokeAllForUser(any(), any());
    }
}
