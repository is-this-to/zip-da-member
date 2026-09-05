package com.zipdamember.domain.auth.service;

import com.zipdamember.domain.auth.entity.LoginSession;
import com.zipdamember.domain.auth.repository.LoginSessionRepository;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.global.cookie.CookieManager;
import com.zipdamember.global.error.custom.business.InvalidTokenException;
import com.zipdamember.global.jwt.JwtConfig;
import com.zipdamember.global.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceLogoutTest {
    private LoginSessionRepository sessions;
    private AuthService service;
    private LoginSession session;
    private String token;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        sessions = mock(LoginSessionRepository.class);
        JwtConfig config = new JwtConfig(false, "test", "JWT", 900000, 600000,
            604800000, "user-refresh-token", "admin-refresh-token", 604800,
            Base64.getEncoder().encodeToString(new byte[32]), "Authorization", "Bearer",
            "/api/member/auth", "/api/member/auth/admin-token-refreshes");
        JwtProvider jwt = new JwtProvider(config);
        service = new AuthService(mock(MemberAccountRepository.class), mock(PasswordEncoder.class),
            sessions, jwt, new CookieManager(config), config);
        MemberAccount member = new MemberAccount();
        member.setMemberId(1L);
        token = jwt.generateRefreshToken(member);
        session = LoginSession.create(1L, token, null, null, null, LocalDateTime.now().plusDays(7));
        when(sessions.findByRefreshToken(token)).thenReturn(Optional.of(session));
        request = new MockHttpServletRequest();
        request.setCookies(new Cookie("user-refresh-token", token));
        response = new MockHttpServletResponse();
    }

    @Test
    void revokesOnlySelectedSessionAndDeletesBothCookiePaths() {
        service.logout(request, response, 1L);
        assertThat(session.isRevoked()).isTrue();
        verify(sessions).findByRefreshToken(token);
        verify(sessions).flush();
        verifyNoMoreInteractions(sessions);
        assertThat(response.getCookies()).hasSize(2).allSatisfy(cookie -> {
            assertThat(cookie.getName()).isEqualTo("user-refresh-token");
            assertThat(cookie.getMaxAge()).isZero();
            assertThat(cookie.isHttpOnly()).isTrue();
        });
        assertThat(response.getCookies()).extracting(Cookie::getPath)
            .containsExactlyInAnyOrder("/api/member/auth", "/api/member/auth/token-refreshes");
    }

    @Test
    void anotherMembersSessionCannotBeRevoked() {
        assertThatThrownBy(() -> service.logout(request, response, 2L)).isInstanceOf(InvalidTokenException.class);
        assertThat(session.isRevoked()).isFalse();
        assertThat(response.getCookies()).isEmpty();
        verify(sessions, never()).flush();
    }

    @Test
    void repeatedLogoutPreservesRevocationTime() {
        session.revoke();
        var revokedAt = session.getRevokedAt();
        service.logout(request, response, 1L);
        assertThat(session.getRevokedAt()).isEqualTo(revokedAt);
        assertThat(response.getCookies()).hasSize(2);
    }

    @Test
    void loggedOutSessionCannotReissue() {
        service.logout(request, response, 1L);
        var reissueResponse = new MockHttpServletResponse();
        assertThatThrownBy(() -> service.reissue(request, reissueResponse)).isInstanceOf(InvalidTokenException.class);
        assertThat(reissueResponse.getCookies()).isEmpty();
    }

    @Test
    void missingCookieIsRejected() {
        assertThatThrownBy(() -> service.logout(new MockHttpServletRequest(), response, 1L))
            .isInstanceOf(InvalidTokenException.class);
        verifyNoInteractions(sessions);
        assertThat(response.getCookies()).isEmpty();
    }

    @Test
    void missingSessionIsRejected() {
        when(sessions.findByRefreshToken(token)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.logout(request, response, 1L)).isInstanceOf(InvalidTokenException.class);
        assertThat(response.getCookies()).isEmpty();
    }

    @Test
    void flushFailureDoesNotDeleteCookie() {
        doThrow(new DataIntegrityViolationException("test failure")).when(sessions).flush();
        assertThatThrownBy(() -> service.logout(request, response, 1L)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(response.getCookies()).isEmpty();
    }
}
