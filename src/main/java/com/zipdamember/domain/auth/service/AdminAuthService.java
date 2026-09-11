package com.zipdamember.domain.auth.service;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.admin.constant.AdminAuditAction;
import com.zipdamember.domain.admin.constant.AdminAuditActorType;
import com.zipdamember.domain.admin.constant.AdminAuditTargetService;
import com.zipdamember.domain.admin.constant.AdminAuditValueType;
import com.zipdamember.domain.admin.entity.Admin;
import com.zipdamember.domain.admin.request.AdminAuditLogWriteRequest;
import com.zipdamember.domain.admin.service.AdminAuditLogService;
import com.zipdamember.domain.auth.entity.AdminLoginSession;
import com.zipdamember.domain.auth.repository.AdminLoginSessionRepository;
import com.zipdamember.domain.admin.repository.AdminRoleAssignmentRepository;
import com.zipdamember.domain.admin.repository.AdminRepository;
import com.zipdamember.domain.auth.request.AdminLoginRequest;
import com.zipdamember.domain.auth.request.AdminPasswordChangeRequest;
import com.zipdamember.domain.auth.response.AdminAuthResponse;
import com.zipdamember.global.error.custom.business.InvalidTokenException;
import com.zipdamember.global.error.custom.business.NotRegisteredException;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;
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
    private final AdminAuditLogService adminAuditLogService;

    @Transactional(rollbackFor = Exception.class)
    public void registerInitialPassword(
            AdminPasswordChangeRequest request,
            String ipAddress,
            String userAgent
    ) {
        // 새 비밀번호 일치 검증
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }

        // 관리자 코드 존재 확인
        Admin admin = adminRepository.findByAdminCode(request.adminCode())
                .orElseThrow(() -> new NotRegisteredException("등록되지 않은 관리자 코드입니다."));

        // 최초 비밀번호 변경 대상 검증
        if (!Boolean.TRUE.equals(admin.getPasswordChangeRequired())) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "최초 비밀번호 변경 대상이 아닙니다."
            );
        }

        // 활성 관리자 역할 조회
        List<AdminRoleCode> roles = adminRoleAssignmentRepository
                .findAllByAdminId(admin.getAdminId()).stream()
                .map(assignment -> assignment.getRoleCode())
                .distinct()
                .toList();

        if (roles.isEmpty()) {
            throw new NotRegisteredException("등록되지 않은 관리자 코드입니다.");
        }

        // 새 비밀번호 암호화 저장
        admin.changePassword(passwordEncoder.encode(request.newPassword()));

        // 최초 비밀번호 변경 감사 로그 저장
        adminAuditLogService.recordSuccess(new AdminAuditLogWriteRequest(
                admin.getAdminId(),
                AdminAuditActorType.ADMIN,
                roles.getFirst(),
                AdminAuditAction.ADMIN_PASSWORD_CHANGE,
                AdminAuditTargetService.MEMBER,
                "ADMIN",
                admin.getAdminId().toString(),
                "최초 관리자 비밀번호 변경",
                ipAddress,
                userAgent,
                List.of(new AdminAuditLogWriteRequest.Change(
                        "passwordChangeRequired",
                        Boolean.TRUE.toString(),
                        Boolean.FALSE.toString(),
                        AdminAuditValueType.BOOLEAN,
                        0
                ))
        ));
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminAuthResponse login(HttpServletRequest request, HttpServletResponse response, AdminLoginRequest adminLoginRequest) {
        // 관리자 계정 조회 및 인증 오류 통일
        Admin admin = adminRepository.findByAdminCode(adminLoginRequest.adminCode())
            .orElseThrow(() -> new NotRegisteredException("아이디와 비밀번호를 확인해주세요."));

        // TODO: 삭제·정지·잠금 상태 관리자와 로그인 실패 횟수/잠금 해제 정책을 검증한다.

        // 일반 로그인 비밀번호 검증
        if (!Boolean.TRUE.equals(admin.getPasswordChangeRequired())
                && (adminLoginRequest.adminPassword() == null
                || adminLoginRequest.adminPassword().isBlank()
                || !passwordEncoder.matches(adminLoginRequest.adminPassword(), admin.getAdminPassword()))) {
            // TODO: 로그인 실패 이력을 기록하고 정책에 따라 계정을 잠근다.

            throw new NotRegisteredException("아이디와 비밀번호를 확인해주세요.");
        }

        // 활성 관리자 역할 조회 및 토큰 반영
        List<AdminRoleCode> roles = adminRoleAssignmentRepository
            .findAllByAdminId(admin.getAdminId()).stream()
            .map(assignment -> assignment.getRoleCode())
            .distinct()
            .toList();

        if (roles.isEmpty()) {
            throw new NotRegisteredException("아이디와 비밀번호를 확인해주세요.");
        }

        // 최초 로그인 비밀번호 설정 안내
        if (Boolean.TRUE.equals(admin.getPasswordChangeRequired())) {
            return new AdminAuthResponse(
                    String.valueOf(admin.getAdminId()),
                    roles,
                    true,
                    null,
                    null
            );
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
            .findAllByAdminId(admin.getAdminId()).stream()
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
