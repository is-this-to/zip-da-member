package com.zipdamember.domain.auth.service;

import com.zipdamember.domain.auth.entity.LoginSession;
import com.zipdamember.domain.auth.repository.LoginSessionRepository;
import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.domain.term.repository.TermAgreementRepository;
import com.zipdamember.domain.term.repository.TermRepository;
import com.zipdamember.domain.verification.repository.VerificationEmailRepository;
import com.zipdamember.domain.verification.util.EmailVerificationHasher;
import com.zipdamember.global.cookie.CookieManager;
import com.zipdamember.global.error.custom.business.InvalidTokenException;
import com.zipdamember.global.jwt.JwtConfig;
import com.zipdamember.global.jwt.JwtProvider;
import com.zipdamember.global.security.constant.MemberRolePolicy;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceReissueTest {
    private final byte[] key = new byte[32];
    private MemberAccountRepository members;
    private LoginSessionRepository sessions;
    private JwtProvider jwt;
    private AuthService service;
    private MemberAccount member;
    private LoginSession session;
    private String oldToken;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        members = mock(MemberAccountRepository.class);
        sessions = mock(LoginSessionRepository.class);
        JwtConfig config = new JwtConfig(false, "test", "JWT", 900000, 600000,
            604800000, "user-refresh-token", "admin-refresh-token", 604800,
            Base64.getEncoder().encodeToString(key), "Authorization", "Bearer",
            "/api/member/auth", "/api/member/auth/admin-token-refreshes");
        jwt = spy(new JwtProvider(config));
        service = new AuthService(
            members, mock(PasswordEncoder.class), sessions, jwt, new CookieManager(config), config,
            mock(TermRepository.class), mock(EmailVerificationHasher.class),
            mock(VerificationEmailRepository.class), mock(TermAgreementRepository.class)
        );
        member = new MemberAccount();
        member.setMemberId(1L);
        oldToken = token("test", "MEMBER", "REFRESH", "1", 60000);
        session = LoginSession.create(1L, oldToken, null, null, null, LocalDateTime.now().plusMinutes(1));
        request = new MockHttpServletRequest();
        request.setCookies(new Cookie("user-refresh-token", oldToken));
        response = new MockHttpServletResponse();
        when(sessions.findByRefreshToken(oldToken)).thenReturn(Optional.of(session));
        when(members.findById(1L)).thenReturn(Optional.of(member));
    }

    private String token(String issuer, String type, String tokenType, String subject, long remainingMs) {
        return Jwts.builder().issuer(issuer).subject(subject)
            .issuedAt(new Date(System.currentTimeMillis() - 5000))
            .expiration(new Date(System.currentTimeMillis() + remainingMs))
            .claim("type", type).claim("tokenType", tokenType).claim("role", "USER")
            .signWith(Keys.hmacShaKeyFor(key)).compact();
    }

    @ParameterizedTest
    @EnumSource(MemberRolePolicy.class)
    void rotatesUsingLatestDatabaseRole(MemberRolePolicy role) {
        member.setMemberRole(role);
        var result = service.reissue(request, response);
        assertThat(result.role()).isEqualTo(role);
        assertThat(jwt.extractClaims(result.accessToken()).get("role")).isEqualTo(role.name());
        assertThat(session.getRefreshToken()).isNotEqualTo(oldToken);
        assertThat(response.getCookie("user-refresh-token").getValue()).isEqualTo(session.getRefreshToken());
        verify(sessions).flush();

        // DB에는 교체된 토큰만 남으므로 이전 토큰 조회 결과를 비운다.
        when(sessions.findByRefreshToken(oldToken)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reissue(request, new MockHttpServletResponse()))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void missingCookieIsRejected() {
        request = new MockHttpServletRequest();
        assertRejected();
        verifyNoInteractions(sessions);
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "access", "subject", "expired", "malformed", "signature"})
    void invalidJwtIsRejected(String scenario) {
        String invalid = switch (scenario) {
            case "admin" -> token("test", "ADMIN", "REFRESH", "1", 60000);
            case "access" -> token("test", "MEMBER", "ACCESS", "1", 60000);
            case "subject" -> token("test", "MEMBER", "REFRESH", "invalid", 60000);
            case "expired" -> token("test", "MEMBER", "REFRESH", "1", -1000);
            case "signature" -> oldToken.substring(0, oldToken.lastIndexOf('.') + 1) + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
            default -> "not-a-jwt";
        };
        request.setCookies(new Cookie("user-refresh-token", invalid));
        assertRejected();
        verify(sessions, never()).findByRefreshToken(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "revoked", "expired", "owner"})
    void invalidSessionIsRejected(String scenario) {
        switch (scenario) {
            case "missing" -> when(sessions.findByRefreshToken(oldToken)).thenReturn(Optional.empty());
            case "revoked" -> session.revoke();
            case "expired" -> ReflectionTestUtils.setField(session, "expiresAt", LocalDateTime.now().minusSeconds(1));
            case "owner" -> ReflectionTestUtils.setField(session, "memberId", 2L);
        }
        assertRejected();
    }

    @ParameterizedTest
    @EnumSource(value = MemberStatus.class, names = {"LOCKED", "SUSPENDED", "WITHDRAWN"})
    void inactiveMemberIsRejected(MemberStatus status) {
        member.setStatus(status);
        assertRejected();
    }

    @Test
    void missingMemberIsRejected() {
        when(members.findById(1L)).thenReturn(Optional.empty());
        assertRejected();
    }

    @Test
    void differentIssuerIsAllowedWhenSignatureAndStoredSessionMatch() {
        String refresh = token("other", "MEMBER", "REFRESH", "1", 60000);
        LoginSession stored = LoginSession.create(1L, refresh, null, null, null, LocalDateTime.now().plusMinutes(1));
        when(sessions.findByRefreshToken(refresh)).thenReturn(Optional.of(stored));
        request.setCookies(new Cookie("user-refresh-token", refresh));
        assertThat(service.reissue(request, response).memberId()).isEqualTo("1");
        assertThat(stored.getRefreshToken()).isNotEqualTo(refresh);
    }

    @Test
    void identicalNewTokenDoesNotMutateSessionOrCookie() {
        doReturn(oldToken).when(jwt).generateRefreshToken(member);
        assertRejected();
    }

    private void assertRejected() {
        assertThatThrownBy(() -> service.reissue(request, response)).isInstanceOf(InvalidTokenException.class);
        assertThat(session.getRefreshToken()).isEqualTo(oldToken);
        assertThat(response.getCookies()).isEmpty();
        verify(sessions, never()).flush();
    }
}
