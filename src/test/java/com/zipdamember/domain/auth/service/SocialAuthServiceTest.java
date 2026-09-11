package com.zipdamember.domain.auth.service;

import com.zipdamember.domain.auth.entity.SocialAccount;
import com.zipdamember.domain.auth.model.SocialSignupClaims;
import com.zipdamember.domain.auth.repository.LoginSessionRepository;
import com.zipdamember.domain.auth.repository.SocialAccountRepository;
import com.zipdamember.domain.auth.request.SocialSignupRequest;
import com.zipdamember.domain.auth.request.TermAgreementRequest;
import com.zipdamember.domain.file.service.FileService;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.domain.term.entity.Term;
import com.zipdamember.domain.term.entity.TermAgreement;
import com.zipdamember.domain.term.repository.TermAgreementRepository;
import com.zipdamember.domain.term.repository.TermRepository;
import com.zipdamember.global.cookie.CookieManager;
import com.zipdamember.global.jwt.JwtProvider;
import com.zipdamember.global.security.constant.ProviderPolicy;
import com.zipdamember.global.security.oauth2.SocialSignupCookieManager;
import com.zipdamember.global.security.oauth2.SocialSignupTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocialAuthServiceTest {

    private static final long MEMBER_ID = 885_000_000_000_000_001L;

    private SocialAccountRepository socialAccountRepository;
    private MemberAccountRepository memberAccountRepository;
    private TermRepository termRepository;
    private TermAgreementRepository termAgreementRepository;
    private FileService fileService;
    private SocialSignupTokenProvider tokenProvider;
    private SocialSignupCookieManager signupCookieManager;
    private JwtProvider jwtProvider;
    private LoginSessionRepository loginSessionRepository;
    private CookieManager cookieManager;
    private SocialAuthService service;

    @BeforeEach
    void setUp() {
        socialAccountRepository = mock(SocialAccountRepository.class);
        memberAccountRepository = mock(MemberAccountRepository.class);
        termRepository = mock(TermRepository.class);
        termAgreementRepository = mock(TermAgreementRepository.class);
        fileService = mock(FileService.class);
        tokenProvider = mock(SocialSignupTokenProvider.class);
        signupCookieManager = mock(SocialSignupCookieManager.class);
        jwtProvider = mock(JwtProvider.class);
        loginSessionRepository = mock(LoginSessionRepository.class);
        cookieManager = mock(CookieManager.class);

        service = new SocialAuthService(
                socialAccountRepository,
                memberAccountRepository,
                loginSessionRepository,
                termRepository,
                termAgreementRepository,
                fileService,
                jwtProvider,
                cookieManager,
                tokenProvider,
                signupCookieManager,
                mock(PasswordEncoder.class)
        );
    }

    @Test
    void signup_savesMemberSocialAccountAndEveryTermSelectionWithoutPassword() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        SocialSignupClaims claims = new SocialSignupClaims(
                ProviderPolicy.KAKAO,
                "3912345678",
                "kakao@example.com",
                "카카오닉네임",
                null
        );
        when(signupCookieManager.get(servletRequest)).thenReturn(Optional.of("signup-token"));
        when(tokenProvider.parse("signup-token")).thenReturn(claims);
        when(socialAccountRepository.existsByProviderAndProviderUserId(ProviderPolicy.KAKAO, "3912345678"))
                .thenReturn(false);
        when(memberAccountRepository.existsByEmail("kakao@example.com")).thenReturn(false);
        when(memberAccountRepository.existsByNickname("집다닉네임")).thenReturn(false);

        Term requiredTerm = term(1L, true);
        Term optionalTerm = term(2L, false);
        when(termRepository.findAllByStatusTrue()).thenReturn(List.of(requiredTerm, optionalTerm));
        when(memberAccountRepository.saveAndFlush(any(MemberAccount.class))).thenAnswer(invocation -> {
            MemberAccount member = invocation.getArgument(0);
            member.setMemberId(MEMBER_ID);
            member.setCreatedAt(LocalDateTime.now());
            return member;
        });
        when(fileService.importKakaoProfile(null, MEMBER_ID)).thenReturn(null);
        Claims refreshClaims = mock(Claims.class);
        when(refreshClaims.getExpiration()).thenReturn(
                Date.from(Instant.now().plus(7, ChronoUnit.DAYS))
        );
        when(jwtProvider.generateRefreshToken(any(MemberAccount.class)))
                .thenReturn("refresh-token");
        when(jwtProvider.extractClaims("refresh-token")).thenReturn(refreshClaims);

        var response = service.signup(
                servletRequest,
                servletResponse,
                new SocialSignupRequest(
                        "홍길동",
                        "집다닉네임",
                        "01012345678",
                        List.of(
                                new TermAgreementRequest("1", "1.0", true),
                                new TermAgreementRequest("2", "1.0", false)
                        )
                )
        );

        assertThat(response.memberId()).isEqualTo(String.valueOf(MEMBER_ID));
        ArgumentCaptor<MemberAccount> memberCaptor = ArgumentCaptor.forClass(MemberAccount.class);
        verify(memberAccountRepository).saveAndFlush(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getPassword()).isNull();
        assertThat(memberCaptor.getValue().getEmailVerificationAt()).isNotNull();
        assertThat(memberCaptor.getValue().getProfileFileId()).isNull();

        ArgumentCaptor<SocialAccount> socialCaptor = ArgumentCaptor.forClass(SocialAccount.class);
        verify(socialAccountRepository).save(socialCaptor.capture());
        assertThat(socialCaptor.getValue().getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(socialCaptor.getValue().getProvider()).isEqualTo(ProviderPolicy.KAKAO);
        assertThat(socialCaptor.getValue().getProviderUserId()).isEqualTo("3912345678");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TermAgreement>> agreementsCaptor = ArgumentCaptor.forClass(List.class);
        verify(termAgreementRepository).saveAll(agreementsCaptor.capture());
        assertThat(agreementsCaptor.getValue())
                .extracting(TermAgreement::getTermId, TermAgreement::getAgreed)
                .containsExactly(tuple(1L, true), tuple(2L, false));
        verify(signupCookieManager).remove(servletResponse);
    }

    private Term term(Long id, boolean required) {
        Term term = new Term();
        term.setTermId(id);
        term.setTermVersion("1.0");
        term.setIsRequired(required);
        term.setStatus(true);
        return term;
    }
}
