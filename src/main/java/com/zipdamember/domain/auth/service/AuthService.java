package com.zipdamember.domain.auth.service;

import com.zipdamember.domain.auth.entity.LoginSession;
import com.zipdamember.domain.auth.repository.LoginSessionRepository;
import com.zipdamember.domain.auth.request.CreateMemberRequest;
import com.zipdamember.domain.auth.request.LoginRequest;
import com.zipdamember.domain.auth.request.TermAgreementRequest;
import com.zipdamember.domain.auth.response.CreateMemberResponse;
import com.zipdamember.domain.auth.response.LoginResponse;
import com.zipdamember.domain.auth.response.MemberPrincipalResponse;
import com.zipdamember.domain.file.service.FileService;
import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.domain.term.entity.Term;
import com.zipdamember.domain.term.entity.TermAgreement;
import com.zipdamember.domain.term.repository.TermAgreementRepository;
import com.zipdamember.domain.term.repository.TermRepository;
import com.zipdamember.domain.verification.constant.EmailVerificationPurposePolicy;
import com.zipdamember.domain.verification.entity.EmailVerification;
import com.zipdamember.domain.verification.repository.VerificationEmailRepository;
import com.zipdamember.domain.verification.util.EmailVerificationHasher;
import com.zipdamember.domain.verification.util.VerificationIdParser;
import com.zipdamember.global.cookie.CookieManager;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.error.custom.business.AlreadyRegisteredException;
import com.zipdamember.global.error.custom.business.InvalidTokenException;
import com.zipdamember.global.error.custom.business.NotRegisteredException;
import com.zipdamember.global.jwt.JwtConfig;
import com.zipdamember.global.jwt.JwtProvider;
import com.zipdamember.global.response.constant.CustomResponseCode;
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
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberAccountRepository memberAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginSessionRepository loginSessionRepository;
    private final JwtProvider jwtProvider;
    private final CookieManager cookieManager;
    private final JwtConfig jwtConfig;
    private final TermRepository termRepository;
    private final EmailVerificationHasher emailVerificationHasher;
    private final VerificationEmailRepository verificationEmailRepository;
    private final TermAgreementRepository termAgreementRepository;
    private final FileService fileService;

    @Transactional(rollbackFor = Exception.class)
    public LoginResponse<MemberPrincipalResponse> login(
            HttpServletRequest request,
            HttpServletResponse response,
            LoginRequest loginRequest
    ) {
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

        return createLoginResponse(accessToken, member);
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
    public LoginResponse<MemberPrincipalResponse> reissue(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
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

        LoginResponse<MemberPrincipalResponse> result = createLoginResponse(accessToken, member);
        session.rotate(newRefreshToken, LocalDateTime.ofInstant(
            jwtProvider.extractClaims(newRefreshToken).getExpiration().toInstant(), ZoneId.systemDefault()
        ));
        // 변경 사항을 DB에 반영하고 DB 오류 확인
        loginSessionRepository.flush();
        // 브라우저 쿠키 교체
        cookieManager.setRefreshTokenToCookie(response, newRefreshToken);
        return result;
    }

    private LoginResponse<MemberPrincipalResponse> createLoginResponse(
            String accessToken,
            MemberAccount member
    ) {
        // 로그인 공통 응답 생성
        return new LoginResponse<>(
                member.getMemberId().toString(),
                member.getMemberRole(),
                accessToken,
                jwtProvider.extractClaims(accessToken).getExpiration().toInstant().atOffset(ZoneOffset.UTC),
                MemberPrincipalResponse.from(member)
        );
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

    /**
     * 사용자 회원가입
     * @param request 저장할 사용자 정보
     * @return 저장한 사용자 정보
     */
    @Transactional
    public CreateMemberResponse signup(CreateMemberRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String nickname = request.nickname().trim();
        Long profileFileId = request.profileFileId() == null ? null : parseProfileFileId(request.profileFileId());
        LocalDateTime now = LocalDateTime.now();

        // 비밀번호랑 비밀번호 확인 비교
        validatePasswordConfirmation(request);

        // 이메일 인증된건지 확인
        EmailVerification emailVerification = validateEmailVerification(
                VerificationIdParser.parse(request.verificationId()),
                normalizedEmail,
                now,
                EmailVerificationPurposePolicy.SIGNUP
        );

        // 중복된 정보인지 확인
        validateMemberDuplicates(normalizedEmail, nickname);

        // 활성 상태인 약관 Map 매핑 반환
        Map<Long, Term> activeTermsById = validateTermAgreements(request.termsAgreements());
        String encodedPassword = passwordEncoder.encode(request.password());

        MemberAccount memberAccount = new MemberAccount();
        memberAccount.setEmail(normalizedEmail);
        memberAccount.setPassword(encodedPassword);
        memberAccount.setName(request.name().trim());
        memberAccount.setNickname(nickname);
        memberAccount.setPhone(request.phone());
        memberAccount.setProfileFileId(profileFileId);
        memberAccount.setEmailVerificationAt(emailVerification.getVerifiedAt());

        MemberAccount savedMemberAccount = memberAccountRepository.save(memberAccount);

        if (profileFileId != null) {
            fileService.assignProfileToMember(
                    profileFileId,
                    savedMemberAccount.getMemberId(),
                    now
            );
        }

        // 화면에 조회된 모든 활성 약관의 선택값(true/false)을 각각 한 행으로 저장한다.
        List<TermAgreement> termAgreementHistory = request.termsAgreements()
                .stream()
                .map(agreement -> TermAgreement.create(
                        savedMemberAccount.getMemberId(),
                        activeTermsById.get(parseTermId(agreement.termsId())).getTermId(),
                        agreement.agreed()
                ))
                .toList();

        termAgreementRepository.saveAll(termAgreementHistory);

        // 가입에 사용한 인증 정보는 재사용할 수 없도록 같은 트랜잭션에서 물리 삭제한다.
        verificationEmailRepository.delete(emailVerification);

        return CreateMemberResponse.from(savedMemberAccount);
    }

    /**
     * 비밀번호와 비밀번호확인 일치여부 확인
     * @param request 회원가입 회원 정보
     */
    private void validatePasswordConfirmation(CreateMemberRequest request) {
        if (!request.password().equals(request.passwordCheck())) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }
    }

    /**
     * 이메일 인증 된 사용자인지 확인하는 메서드
     * @param verificationId 이메일 인증 식별자
     * @param normalizedEmail 이메일 평문
     * @param now 현재 시간
     * @return 이메일 인증 데이터
     */
    private EmailVerification validateEmailVerification(Long verificationId, String normalizedEmail, LocalDateTime now, EmailVerificationPurposePolicy policy) {
        // 해당 식별자를 가진 이메일 인증 정보가 없음
        EmailVerification emailVerification = verificationEmailRepository.findByIdForUpdate(verificationId)
                .orElseThrow(() -> new BusinessException(
                        CustomResponseCode.EMAIL_VERIFICATION_NOT_FOUND,
                        "이메일 인증 정보를 찾을 수 없습니다."
                ));

        // 이메일 인증
        if (emailVerification.getPurpose() != policy) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "회원가입용 이메일 인증 정보가 아닙니다."
            );
        }

        // 인증 완료한 이메일과 회원가입 이메일 일치하지 않음
        String emailHash = emailVerificationHasher.hashEmail(normalizedEmail);
        if (!emailHash.equals(emailVerification.getVerificationEmail())) {
            throw new BusinessException(
                    CustomResponseCode.EMAIL_VERIFICATION_EMAIL_MISMATCH,
                    "인증을 완료한 이메일과 회원가입 이메일이 일치하지 않습니다."
            );
        }

        // 인증이 완료됐는가?
        if (!emailVerification.isVerified()) {
            throw new BusinessException(
                    CustomResponseCode.EMAIL_VERIFICATION_REQUIRED,
                    "이메일 인증을 먼저 완료해야 합니다."
            );
        }

        return emailVerification;
    }

    /**
     * DB 중복 저장 방지용 이메일, 닉네임 확인
     * @param normalizedEmail 이메일 평문
     * @param nickname 닉네임
     */
    private void validateMemberDuplicates(String normalizedEmail, String nickname) {
        if (memberAccountRepository.existsByEmail(normalizedEmail)) {
            throw new AlreadyRegisteredException("이미 가입된 이메일입니다.");
        }

        if (memberAccountRepository.existsByNickname(nickname)) {
            throw new BusinessException(
                    CustomResponseCode.DUPLICATED_RESOURCE_ERROR,
                    "이미 사용 중인 닉네임입니다."
            );
        }
    }

    /**
     * 사용자가 보낸 termsId가 현재 활성 약관인지 확인, 약관 버전 확인, 필수 약관인지 확인
     * @param requestedAgreements 사용자가 동의, 비동의한 이용 약관 모음
     * @return 활성화된 모든 약관 동의, 비동의 Map
     */
    private Map<Long, Term> validateTermAgreements(List<TermAgreementRequest> requestedAgreements) {
        Set<Long> requestedTermIds = new HashSet<>();

        // 사용자 동의 약관 Set에 넣기
        for (TermAgreementRequest agreement : requestedAgreements) {
            if (!requestedTermIds.add(parseTermId(agreement.termsId()))) {
                throw new BusinessException(CustomResponseCode.INVALID_PARAMETER_ERROR, "동일한 약관에 대한 동의 내역이 중복되었습니다.");
            }
        }

        // 활성 약관 Map에 넣기
        List<Term> activeTerms = termRepository.findAllByStatusTrue();
        Map<Long, Term> activeTermsById = new HashMap<>();
        for (Term term : activeTerms) {
            activeTermsById.put(term.getTermId(), term);
        }

        // 활성화된 모든 약관에 대해서 선택을 했는가?
        if (!requestedTermIds.equals(activeTermsById.keySet())) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "조회된 활성 약관 전체의 동의 여부를 전달해야 합니다."
            );
        }

        for (TermAgreementRequest agreement : requestedAgreements) {
            // 활성화된 약관에서 사용자 선택한 약관을 key로 선택
            Term term = activeTermsById.get(parseTermId(agreement.termsId()));

            // 버전 비교
            if (!term.getTermVersion().equals(agreement.version())) {
                throw new BusinessException(CustomResponseCode.INVALID_PARAMETER_ERROR, "현재 약관 버전과 요청한 약관 버전이 일치하지 않습니다.");
            }

            // 필수 약관인지 사용자가 동의하지 않은 경우에만 예외 발생
            if (Boolean.TRUE.equals(term.getIsRequired()) && !Boolean.TRUE.equals(agreement.agreed())) {
                throw new BusinessException(CustomResponseCode.INVALID_PARAMETER_ERROR, "필수 약관에 모두 동의해야 합니다.");
            }
        }

        return activeTermsById;
    }

    private Long parseTermId(String termsId) {
        try {
            return Long.parseLong(termsId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "올바르지 않은 약관 식별자입니다."
            );
        }
    }

    private Long parseProfileFileId(String profileFileId) {
        try {
            return Long.parseLong(profileFileId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "올바르지 않은 프로필 파일 식별자입니다."
            );
        }
    }
}
