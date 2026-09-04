package com.zipdamember.domain.auth.service;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.admin.entity.Admin;
import com.zipdamember.domain.auth.entity.AdminLoginSession;
import com.zipdamember.domain.auth.repository.AdminLoginSessionRepository;
import com.zipdamember.domain.admin.repository.AdminRoleAssignmentRepository;
import com.zipdamember.domain.admin.repository.AdminRepository;
import com.zipdamember.domain.auth.request.AdminLoginRequest;
import com.zipdamember.domain.auth.response.AdminAuthResponse;
import com.zipdamember.global.error.custom.business.InvalidTokenException;
import com.zipdamember.global.error.custom.business.NotRegisteredException;
import com.zipdamember.global.jwt.JwtConfig;
import com.zipdamember.global.jwt.JwtProvider;
import com.zipdamember.global.jwt.request.AdminTokenGenerateRequest;
import io.jsonwebtoken.Claims;
import com.zipdamember.global.cookie.CookieManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private final AdminRepository adminRepository;
    private final AdminLoginSessionRepository adminLoginSessionRepository;
    private final AdminRoleAssignmentRepository adminRoleAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtConfig jwtConfig;
    private final CookieManager cookieManager;

    @Transactional(rollbackFor = Exception.class)
    public AdminAuthResponse login(HttpServletRequest request, HttpServletResponse response, AdminLoginRequest adminLoginRequest) {
        // 관리자 계정 조회 및 인증 오류 통일
        Admin admin = adminRepository.findByAdminCode(adminLoginRequest.adminCode())
            .orElseThrow(() -> new NotRegisteredException("아이디와 비밀번호를 확인해주세요."));

        // TODO: 삭제·정지·잠금 상태 관리자와 로그인 실패 횟수/잠금 해제 정책을 검증한다.

        // 비밀번호 체크
        if(!passwordEncoder.matches(adminLoginRequest.adminPassword(), admin.getAdminPassword())) {
            // TODO: 로그인 실패 이력을 기록하고 정책에 따라 계정을 잠근다.

            throw new NotRegisteredException("아이디와 비밀번호를 확인해주세요.");
        }

        // 활성 관리자 역할 조회 및 토큰 반영
        List<AdminRoleCode> roles = adminRoleAssignmentRepository
            .findAllByAdminIdAndDeletedAtIsNull(admin.getAdminId()).stream()
            .map(assignment -> assignment.getRoleCode())
            .distinct()
            .toList();

        if (roles.isEmpty()) {
            throw new NotRegisteredException("아이디와 비밀번호를 확인해주세요.");
        }

        // TODO: 로그인 성공 이력(IP, User-Agent, 로그인 유형)을 기록한다.

        // 관리자 Access/Refresh Token 발급
        AdminTokenGenerateRequest tokenRequest = new AdminTokenGenerateRequest(admin.getAdminId(), roles);
        String accessToken = jwtProvider.generateAdminAccessToken(tokenRequest);
        String refreshToken = jwtProvider.generateAdminRefreshToken(tokenRequest);

        // Refresh Token 원문 및 접속 정보 세션 저장
        AdminLoginSession session = AdminLoginSession.create(
            admin.getAdminId(),
            refreshToken,
            request.getHeader("X-Device-Id"),
            request.getHeader("User-Agent"),
            request.getRemoteAddr(),
            LocalDateTime.now().plus(Duration.ofMillis(jwtConfig.refreshTokenExpiryMs()))
        );
        adminLoginSessionRepository.save(session);
        cookieManager.setAdminRefreshTokenToCookie(response, refreshToken);

        return new AdminAuthResponse(
            String.valueOf(admin.getAdminId()),
            roles,
            Boolean.TRUE.equals(admin.getPasswordChangeRequired()),
            accessToken,
            OffsetDateTime.now().plus(Duration.ofMillis(jwtConfig.adminAccessTokenExpiryMs()))
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminAuthResponse reissue(HttpServletRequest request, HttpServletResponse response) {
        // 관리자 전용 Refresh Token 쿠키 조회
        String refreshToken = cookieManager.getAdminRefreshTokenToCookie(request)
            .orElseThrow(() -> new InvalidTokenException("리프래시 토큰 없음"));

        // 관리자 Refresh Token Claims 검증
        Claims claims = jwtProvider.extractClaims(refreshToken);
        if (!jwtConfig.issuer().equals(claims.getIssuer())
            || !"ADMIN".equals(claims.get("type", String.class))
            || !"REFRESH".equals(claims.get("tokenType", String.class))) {
            throw new InvalidTokenException("관리자 Refresh Token이 아닙니다.");
        }

        long adminId;
        try {
            adminId = Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidTokenException("유효하지 않은 관리자 식별자입니다.");
        }

        // 관리자 로그인 세션 잠금 조회
        AdminLoginSession session = adminLoginSessionRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new InvalidTokenException("유효하지 않은 관리자 세션입니다."));

        if (!session.getAdminId().equals(adminId) || session.isExpired() || session.isRevoked()) {
            throw new InvalidTokenException("만료되었거나 폐기된 관리자 세션입니다.");
        }

        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new InvalidTokenException("유효하지 않은 관리자 토큰입니다."));

        // 활성 관리자 역할 조회 및 토큰 반영
        List<AdminRoleCode> roles = adminRoleAssignmentRepository
            .findAllByAdminIdAndDeletedAtIsNull(admin.getAdminId()).stream()
            .map(assignment -> assignment.getRoleCode())
            .distinct()
            .toList();

        if (roles.isEmpty()) {
            throw new InvalidTokenException("활성 관리자 역할이 없습니다.");
        }

        // 최신 역할 기반 관리자 토큰 재발급
        AdminTokenGenerateRequest tokenRequest = new AdminTokenGenerateRequest(admin.getAdminId(), roles);
        String accessToken = jwtProvider.generateAdminAccessToken(tokenRequest);
        String newRefreshToken = jwtProvider.generateAdminRefreshToken(tokenRequest);

        // 관리자 로그인 세션의 새 Refresh Token 교체
        session.rotate(
            newRefreshToken,
            LocalDateTime.now().plus(Duration.ofMillis(jwtConfig.refreshTokenExpiryMs()))
        );
        cookieManager.setAdminRefreshTokenToCookie(response, newRefreshToken);

        return new AdminAuthResponse(
            String.valueOf(admin.getAdminId()),
            roles,
            Boolean.TRUE.equals(admin.getPasswordChangeRequired()),
            accessToken,
            OffsetDateTime.now().plus(Duration.ofMillis(jwtConfig.adminAccessTokenExpiryMs()))
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void logout(HttpServletRequest request, HttpServletResponse response, long adminId) {
        // 현재 관리자 로그인 세션 폐기
        String refreshToken = cookieManager.getAdminRefreshTokenToCookie(request)
            .orElseThrow(() -> new InvalidTokenException("리프래시 토큰 없음"));

        // 관리자 로그인 세션 잠금 조회
        AdminLoginSession session = adminLoginSessionRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new InvalidTokenException("유효하지 않은 관리자 세션입니다."));

        if (!session.getAdminId().equals(adminId)) {
            throw new InvalidTokenException("관리자 세션 정보가 일치하지 않습니다.");
        }

        session.revoke();
        cookieManager.removeAdminRefreshTokenToCookie(response);
    }
}
