package com.zipdamember.domain.auth.service;

import com.zipdamember.domain.auth.entity.LoginSession;
import com.zipdamember.domain.auth.repository.LoginSessionRepository;
import com.zipdamember.domain.auth.request.LoginRequest;
import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.domain.term.repository.TermAgreementRepository;
import com.zipdamember.domain.term.repository.TermRepository;
import com.zipdamember.domain.verification.repository.VerificationEmailRepository;
import com.zipdamember.domain.verification.util.EmailVerificationHasher;
import com.zipdamember.global.cookie.CookieManager;
import com.zipdamember.global.error.custom.business.NotRegisteredException;
import com.zipdamember.global.jwt.JwtConfig;
import com.zipdamember.global.jwt.JwtProvider;
import com.zipdamember.global.security.constant.MemberRolePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Base64;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private MemberAccountRepository members;
    private LoginSessionRepository sessions;
    private PasswordEncoder encoder;
    private JwtProvider jwt;
    private AuthService service;
    private MemberAccount member;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        members = mock(MemberAccountRepository.class);
        sessions = mock(LoginSessionRepository.class);
        encoder = mock(PasswordEncoder.class);
        JwtConfig config = new JwtConfig(false, "test", "JWT", 900000, 600000,
            604800000, "user-refresh-token", "admin-refresh-token", 604800,
            Base64.getEncoder().encodeToString(new byte[32]), "Authorization", "Bearer",
            "/api/member/auth", "/api/member/auth/admin-token-refreshes");
        jwt = new JwtProvider(config);
        service = new AuthService(
            members, encoder, sessions, jwt, new CookieManager(config), config,
            mock(TermRepository.class), mock(EmailVerificationHasher.class),
            mock(VerificationEmailRepository.class), mock(TermAgreementRepository.class)
        );
        member = new MemberAccount();
        member.setMemberId(1L);
        member.setPassword("encoded-password");
        request = new MockHttpServletRequest();
        request.addHeader("X-Device-Id", "test-device");
        request.addHeader("User-Agent", "test-agent");
        response = new MockHttpServletResponse();
        when(members.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(encoder.matches("Test1234!", "encoded-password")).thenReturn(true);
    }

    @ParameterizedTest
    @EnumSource(MemberRolePolicy.class)
    void successfulLoginStoresSessionAndSetsCookie(MemberRolePolicy role) {
        member.setMemberRole(role);
        var result = service.login(request, response, new LoginRequest("member@example.com", "Test1234!"));
        ArgumentCaptor<LoginSession> saved = ArgumentCaptor.forClass(LoginSession.class);
        verify(sessions).saveAndFlush(saved.capture());
        var cookie = response.getCookie("user-refresh-token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getMaxAge()).isEqualTo(604800);
        assertThat(cookie.getPath()).isEqualTo("/api/member/auth");
        assertThat(response.getCookies()).anySatisfy(legacy -> {
            assertThat(legacy.getName()).isEqualTo("user-refresh-token");
            assertThat(legacy.getPath()).isEqualTo("/api/member/auth/token-refreshes");
            assertThat(legacy.getMaxAge()).isZero();
        });
        assertThat(saved.getValue().getRefreshToken()).isEqualTo(cookie.getValue());
        assertThat(saved.getValue().getMemberId()).isEqualTo(1L);
        assertThat(saved.getValue().getDeviceId()).isEqualTo("test-device");
        assertThat(result.memberId()).isEqualTo("1");
        assertThat(result.role()).isEqualTo(role);
        assertThat(jwt.extractClaims(result.accessToken()).get("role", String.class)).isEqualTo(role.name());
        assertThat(result.accessTokenExpiresAt().toInstant()).isEqualTo(jwt.extractClaims(result.accessToken()).getExpiration().toInstant());
    }

    @Test
    void wrongPasswordDoesNotSaveSession() {
        assertThatThrownBy(() -> service.login(request, response, new LoginRequest("member@example.com", "Wrong1234!")))
            .isInstanceOf(NotRegisteredException.class);
        verifyNoInteractions(sessions);
        assertThat(response.getCookies()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = MemberStatus.class, names = {"LOCKED", "SUSPENDED", "WITHDRAWN"})
    void inactiveMemberCannotLogin(MemberStatus status) {
        member.setStatus(status);
        assertThatThrownBy(() -> service.login(request, response, new LoginRequest("member@example.com", "Test1234!")))
            .isInstanceOf(NotRegisteredException.class);
        verifyNoInteractions(sessions);
        assertThat(response.getCookies()).isEmpty();
    }

    @Test
    void databaseFailureDoesNotSetCookie() {
        when(sessions.saveAndFlush(any(LoginSession.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThatThrownBy(() -> service.login(request, response, new LoginRequest("member@example.com", "Test1234!")))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(response.getCookies()).isEmpty();
    }
}
