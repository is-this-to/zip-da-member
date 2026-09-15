package com.zipdamember.domain.auth.service;

import com.zipdamember.domain.auth.entity.LoginSession;
import com.zipdamember.domain.auth.entity.SocialAccount;
import com.zipdamember.domain.auth.model.SocialSignupClaims;
import com.zipdamember.domain.auth.repository.LoginSessionRepository;
import com.zipdamember.domain.auth.repository.SocialAccountRepository;
import com.zipdamember.domain.auth.request.SocialSignupRequest;
import com.zipdamember.domain.auth.request.SocialAccountLinkRequest;
import com.zipdamember.domain.auth.request.TermAgreementRequest;
import com.zipdamember.domain.auth.response.CreateMemberResponse;
import com.zipdamember.domain.auth.response.SocialSignupContextResponse;
import com.zipdamember.domain.file.service.FileService;
import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.domain.term.entity.Term;
import com.zipdamember.domain.term.entity.TermAgreement;
import com.zipdamember.domain.term.repository.TermAgreementRepository;
import com.zipdamember.domain.term.repository.TermRepository;
import com.zipdamember.global.cookie.CookieManager;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.error.custom.business.AlreadyRegisteredException;
import com.zipdamember.global.error.custom.business.InvalidTokenException;
import com.zipdamember.global.jwt.JwtProvider;
import com.zipdamember.global.response.constant.CustomResponseCode;
import com.zipdamember.global.security.oauth2.SocialSignupCookieManager;
import com.zipdamember.global.security.oauth2.SocialSignupTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final SocialAccountRepository socialAccountRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final TermRepository termRepository;
    private final TermAgreementRepository termAgreementRepository;
    private final FileService fileService;
    private final JwtProvider jwtProvider;
    private final CookieManager cookieManager;
    private final SocialSignupTokenProvider socialSignupTokenProvider;
    private final SocialSignupCookieManager socialSignupCookieManager;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public SocialSignupContextResponse getSignupContext(HttpServletRequest request) {
        SocialSignupClaims claims = getSignupClaims(request);
        return new SocialSignupContextResponse(
                claims.email(),
                claims.nickname(),
                claims.profileImageUrl()
        );
    }

    @Transactional
    public CreateMemberResponse signup(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse,
            SocialSignupRequest request
    ) {
        SocialSignupClaims claims = getSignupClaims(servletRequest);
        String nickname = request.nickname().trim();
        LocalDateTime now = LocalDateTime.now();

        if (socialAccountRepository.existsByProviderAndProviderUserId(
                claims.provider(), claims.providerUserId())) {
            throw new AlreadyRegisteredException("이미 가입된 소셜 계정입니다.");
        }
        if (memberAccountRepository.existsByEmail(claims.email())) {
            throw new AlreadyRegisteredException("이미 가입된 이메일입니다.");
        }
        if (memberAccountRepository.existsByNickname(nickname)) {
            throw new BusinessException(
                    CustomResponseCode.DUPLICATED_RESOURCE_ERROR,
                    "이미 사용 중인 닉네임입니다."
            );
        }

        Map<Long, Term> activeTermsById = validateTermAgreements(request.termsAgreements());

        MemberAccount member = new MemberAccount();
        member.setEmail(claims.email());
        member.setPassword(null);
        member.setName(request.name().trim());
        member.setNickname(nickname);
        member.setPhone(request.phone());
        member.setEmailVerificationAt(now);

        MemberAccount savedMember = memberAccountRepository.saveAndFlush(member);
        Long profileFileId = fileService.importKakaoProfile(
                claims.profileImageUrl(),
                savedMember.getMemberId()
        );
        savedMember.setProfileFileId(profileFileId);

        socialAccountRepository.save(SocialAccount.create(
                savedMember.getMemberId(),
                claims.provider(),
                claims.providerUserId(),
                claims.email()
        ));

        List<TermAgreement> agreements = request.termsAgreements().stream()
                .map(agreement -> TermAgreement.create(
                        savedMember.getMemberId(),
                        activeTermsById.get(parseTermId(agreement.termsId())).getTermId(),
                        agreement.agreed()
                ))
                .toList();
        termAgreementRepository.saveAll(agreements);

        // 회원가입 저장을 확정한 뒤 로그인 세션까지 발급해 한 번의 소셜 흐름으로 완료한다.
        socialAccountRepository.flush();
        termAgreementRepository.flush();
        socialSignupCookieManager.remove(servletResponse);
        issueLoginSession(servletRequest, servletResponse, savedMember);
        return CreateMemberResponse.from(savedMember);
    }

    @Transactional
    public void linkLocalAccount(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse,
            SocialAccountLinkRequest request
    ) {
        SocialSignupClaims claims = getSignupClaims(servletRequest);
        if (!request.agreed()) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "소셜 계정 연결에 동의해야 합니다."
            );
        }
        if (socialAccountRepository.existsByProviderAndProviderUserId(
                claims.provider(), claims.providerUserId())) {
            throw new AlreadyRegisteredException("이미 연결된 소셜 계정입니다.");
        }

        MemberAccount member = memberAccountRepository.findByEmail(claims.email())
                .orElseThrow(() -> new InvalidTokenException("연결할 기존 회원을 찾을 수 없습니다."));
        if (member.getPassword() == null
                || !passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(
                    CustomResponseCode.UNAUTHENTICATED_ERROR,
                    "기존 계정 비밀번호가 일치하지 않습니다."
            );
        }
        if (member.getStatus() != MemberStatus.ACTIVE || member.getWithdrawnAt() != null) {
            throw new BusinessException(
                    CustomResponseCode.UNAUTHORIZED_ERROR,
                    "현재 연결할 수 없는 회원입니다."
            );
        }

        socialAccountRepository.saveAndFlush(SocialAccount.create(
                member.getMemberId(),
                claims.provider(),
                claims.providerUserId(),
                claims.email()
        ));
        socialSignupCookieManager.remove(servletResponse);
        issueLoginSession(servletRequest, servletResponse, member);
    }

    @Transactional
    public void loginExistingMember(
            HttpServletRequest request,
            HttpServletResponse response,
            Long memberId
    ) {
        MemberAccount member = memberAccountRepository.findById(memberId)
                .orElseThrow(() -> new InvalidTokenException("연결된 회원을 찾을 수 없습니다."));
        if (member.getStatus() != MemberStatus.ACTIVE || member.getWithdrawnAt() != null) {
            throw new InvalidTokenException("현재 로그인할 수 없는 회원입니다.");
        }

        issueLoginSession(request, response, member);
    }

    private void issueLoginSession(
            HttpServletRequest request,
            HttpServletResponse response,
            MemberAccount member
    ) {
        String refreshToken = jwtProvider.generateRefreshToken(member);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                jwtProvider.extractClaims(refreshToken).getExpiration().toInstant(),
                ZoneId.systemDefault()
        );
        LoginSession session = LoginSession.create(
                member.getMemberId(),
                refreshToken,
                request.getHeader("X-Device-Id"),
                request.getHeader("User-Agent"),
                request.getRemoteAddr(),
                expiresAt
        );
        loginSessionRepository.saveAndFlush(session);
        cookieManager.setRefreshTokenToCookie(response, refreshToken);
    }

    private SocialSignupClaims getSignupClaims(HttpServletRequest request) {
        String token = socialSignupCookieManager.get(request)
                .orElseThrow(() -> new InvalidTokenException("카카오 로그인을 먼저 진행해주세요."));
        return socialSignupTokenProvider.parse(token);
    }

    private Map<Long, Term> validateTermAgreements(List<TermAgreementRequest> requestedAgreements) {
        Set<Long> requestedTermIds = new HashSet<>();
        for (TermAgreementRequest agreement : requestedAgreements) {
            if (!requestedTermIds.add(parseTermId(agreement.termsId()))) {
                throw new BusinessException(
                        CustomResponseCode.INVALID_PARAMETER_ERROR,
                        "동일한 약관에 대한 동의 내역이 중복되었습니다."
                );
            }
        }

        Map<Long, Term> activeTermsById = new HashMap<>();
        for (Term term : termRepository.findAllByStatusTrue()) {
            activeTermsById.put(term.getTermId(), term);
        }
        if (!requestedTermIds.equals(activeTermsById.keySet())) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "조회된 활성 약관 전체의 동의 여부를 전달해야 합니다."
            );
        }

        for (TermAgreementRequest agreement : requestedAgreements) {
            Term term = activeTermsById.get(parseTermId(agreement.termsId()));
            if (!term.getTermVersion().equals(agreement.version())) {
                throw new BusinessException(
                        CustomResponseCode.INVALID_PARAMETER_ERROR,
                        "현재 약관 버전과 요청한 약관 버전이 일치하지 않습니다."
                );
            }
            if (Boolean.TRUE.equals(term.getIsRequired())
                    && !Boolean.TRUE.equals(agreement.agreed())) {
                throw new BusinessException(
                        CustomResponseCode.INVALID_PARAMETER_ERROR,
                        "필수 약관에 모두 동의해야 합니다."
                );
            }
        }
        return activeTermsById;
    }

    private Long parseTermId(String termId) {
        try {
            return Long.parseLong(termId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "올바르지 않은 약관 식별자입니다."
            );
        }
    }
}
