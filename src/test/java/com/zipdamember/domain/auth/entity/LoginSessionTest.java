package com.zipdamember.domain.auth.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

class LoginSessionTest {
    private LoginSession session() {
        return LoginSession.create(1L, "old-token", null, null, null, LocalDateTime.now().plusDays(7));
    }

    @Test
    void createStoresOriginalToken() {
        LoginSession session = session();
        assertThat(session.getMemberId()).isEqualTo(1L);
        assertThat(session.getRefreshToken()).isEqualTo("old-token");
        assertThat(session.isExpired()).isFalse();
        assertThat(session.isRevoked()).isFalse();
    }

    @Test
    void invalidInputIsRejected() {
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        assertThatIllegalArgumentException().isThrownBy(() -> LoginSession.create(null, "token", null, null, null, future));
        assertThatIllegalArgumentException().isThrownBy(() -> LoginSession.create(0L, "token", null, null, null, future));
        assertThatIllegalArgumentException().isThrownBy(() -> LoginSession.create(1L, " ", null, null, null, future));
        assertThatIllegalArgumentException().isThrownBy(() -> LoginSession.create(1L, "token", null, null, null, null));
    }

    @Test
    void rotationReplacesTokenAndExpiry() {
        LoginSession session = session();
        LocalDateTime expiry = LocalDateTime.now().plusDays(8);
        session.rotate("new-token", expiry);
        assertThat(session.getRefreshToken()).isEqualTo("new-token");
        assertThat(session.getExpiresAt()).isEqualTo(expiry);
    }

    @Test
    void revokedSessionCannotRotate() {
        LoginSession session = session();
        session.revoke();
        assertThat(session.isRevoked()).isTrue();
        assertThatIllegalStateException().isThrownBy(() -> session.rotate("new-token", LocalDateTime.now().plusDays(1)));
    }

    @Test
    void expiredSessionCannotRotate() {
        LoginSession session = session();
        ReflectionTestUtils.setField(session, "expiresAt", LocalDateTime.now().minusSeconds(1));
        assertThat(session.isExpired()).isTrue();
        assertThatIllegalStateException().isThrownBy(() -> session.rotate("new-token", LocalDateTime.now().plusDays(1)));
    }

    @Test
    void sameTokenCannotRotate() {
        LoginSession session = session();
        assertThatIllegalArgumentException().isThrownBy(() -> session.rotate("old-token", LocalDateTime.now().plusDays(1)));
        assertThat(session.getRefreshToken()).isEqualTo("old-token");
    }
}
