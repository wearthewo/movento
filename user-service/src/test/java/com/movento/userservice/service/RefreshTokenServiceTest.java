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
import static org.mockito.Mockito.verify;

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
}
