package com.zipdamember.domain.auth.service;

import com.zipdamember.domain.auth.entity.LoginSession;
import com.zipdamember.domain.auth.repository.LoginSessionRepository;
import com.zipdamember.domain.auth.request.LoginRequest;
import com.zipdamember.domain.auth.response.LoginResponse;
import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.global.cookie.CookieManager;
import com.zipdamember.global.error.custom.business.InvalidTokenException;
import com.zipdamember.global.error.custom.business.NotRegisteredException;
import com.zipdamember.global.jwt.JwtConfig;
import com.zipdamember.global.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberAccountRepository memberAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginSessionRepository loginSessionRepository;
    private final JwtProvider jwtProvider;
    private final CookieManager cookieManager;
    private final JwtConfig jwtConfig;

    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(HttpServletRequest request, HttpServletResponse response, LoginRequest loginRequest) {
        MemberAccount member = authenticate(loginRequest);
        String accessToken = jwtProvider.generateAccessToken(member);
        String refreshToken = jwtProvider.generateRefreshToken(member);

        // JWT에 기록된 실제 만료 시각으로 세션·응답의 만료 시각을 맞춤
        LocalDateTime sessionExpiresAt = LocalDateTime.ofInstant(
            jwtProvider.extractClaims(refreshToken).getExpiration().toInstant(), ZoneId.systemDefault()
        );
        LoginSession session = LoginSession.create(
            member.getMemberId(), refreshToken,
            request.getHeader("X-Device-Id"), request.getHeader("User-Agent"),
            request.getRemoteAddr(), sessionExpiresAt
        );
        // 쿠키를 설정하기 전 UNIQUE 위반 등 저장 오류 확인
        loginSessionRepository.saveAndFlush(session);
        cookieManager.setRefreshTokenToCookie(response, refreshToken);

        return new LoginResponse(
            String.valueOf(member.getMemberId()), member.getMemberRole(), accessToken,
            jwtProvider.extractClaims(accessToken).getExpiration().toInstant().atOffset(ZoneOffset.UTC)
        );
    }

    @Transactional(readOnly = true)
    public MemberAccount authenticate(LoginRequest request) {
        String normalizedEmail = request.email().strip().toLowerCase(Locale.ROOT);

        MemberAccount member = memberAccountRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new NotRegisteredException("이메일과 비밀번호를 확인해주세요."));

        if (member.getPassword() == null
            || !passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new NotRegisteredException("이메일과 비밀번호를 확인해주세요.");
        }

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new NotRegisteredException("이메일과 비밀번호를 확인해주세요.");
        }

        return member;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginResponse reissue(HttpServletRequest request, HttpServletResponse response) {
        // 회원 Refresh Token만 재발급 자격 증명으로 사용
        String refreshToken = cookieManager.getRefreshTokenToCookie(request)
            .filter(token -> !token.isBlank())
            .orElseThrow(() -> new InvalidTokenException("리프레시 토큰 없음"));

        Claims claims = jwtProvider.extractClaims(refreshToken);
        if (!"MEMBER".equals(claims.get("type"))
            || !"REFRESH".equals(claims.get("tokenType"))
            || claims.getExpiration() == null) {
            throw new InvalidTokenException("회원 Refresh Token이 아닙니다.");
        }

        long memberId;
        try {
            memberId = Long.parseLong(claims.getSubject());
            if (memberId <= 0) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("유효하지 않은 회원 식별자입니다.");
        }

        // 같은 트랜잭션에서 잠금 조회부터 토큰 교체까지 처리
        LoginSession session = loginSessionRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new InvalidTokenException("유효하지 않은 로그인 세션입니다."));
        if (!session.getMemberId().equals(memberId) || session.isExpired() || session.isRevoked()) {
            throw new InvalidTokenException("만료되었거나 폐기된 로그인 세션입니다.");
        }

        MemberAccount member = memberAccountRepository.findById(memberId)
            .orElseThrow(() -> new InvalidTokenException("유효하지 않은 회원입니다."));
        if (member.getStatus() != MemberStatus.ACTIVE || member.getWithdrawnAt() != null) {
            throw new InvalidTokenException("토큰을 재발급할 수 없는 회원입니다.");
        }

        // 최신 역할 기반 회원 토큰 재발급
        String accessToken = jwtProvider.generateAccessToken(member);
        String newRefreshToken = jwtProvider.generateRefreshToken(member);
        if (refreshToken.equals(newRefreshToken)) {
            throw new InvalidTokenException("동일한 Refresh Token이 생성되었습니다. 잠시 후 다시 요청해주세요.");
        }

        LoginResponse result = new LoginResponse(
            String.valueOf(memberId), member.getMemberRole(), accessToken,
            jwtProvider.extractClaims(accessToken).getExpiration().toInstant().atOffset(ZoneOffset.UTC)
        );
        session.rotate(newRefreshToken, LocalDateTime.ofInstant(
            jwtProvider.extractClaims(newRefreshToken).getExpiration().toInstant(), ZoneId.systemDefault()
        ));
        // 변경 사항을 DB에 반영하고 DB 오류 확인
        loginSessionRepository.flush();
        // 브라우저 쿠키 교체
        cookieManager.setRefreshTokenToCookie(response, newRefreshToken);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void logout(HttpServletRequest request, HttpServletResponse response, long memberId) {
        // 현재 로그인 세션 폐기
        String refreshToken = cookieManager.getRefreshTokenToCookie(request)
            .filter(token -> !token.isBlank())
            .orElseThrow(() -> new InvalidTokenException("리프레시 토큰 없음"));
        LoginSession session = loginSessionRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new InvalidTokenException("유효하지 않은 로그인 세션입니다."));
        if (!session.getMemberId().equals(memberId)) {
            throw new InvalidTokenException("회원 세션 정보가 일치하지 않습니다.");
        }

        // 중복 로그아웃은 폐기 시각을 유지하고 쿠키만 정리
        if (!session.isRevoked()) {
            session.revoke();
        }
        // 세션 폐기 상태를 DB에 반영하고 DB 오류 확인
        loginSessionRepository.flush();
        // 브라우저의 Refresh Token 쿠키 제거
        cookieManager.removeRefreshTokenToCookie(response);
    }
}
