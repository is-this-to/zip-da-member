package com.zipdamember.domain.auth.service;

import com.zipdamember.domain.auth.request.CreateMemberRequest;
import com.zipdamember.domain.auth.request.TermAgreementRequest;
import com.zipdamember.domain.auth.repository.LoginSessionRepository;
import com.zipdamember.domain.file.service.FileService;
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
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.cookie.CookieManager;
import com.zipdamember.global.jwt.JwtConfig;
import com.zipdamember.global.jwt.JwtProvider;
import com.zipdamember.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceSignupTest {

    private static final Long VERIFICATION_ID = 100L;
    private static final Long MEMBER_ID = 1000L;
    private static final String EMAIL = "User@Example.com";
    private static final String NORMALIZED_EMAIL = "user@example.com";
    private static final String EMAIL_HASH = "email-hash";

    private VerificationEmailRepository verificationEmailRepository;
    private MemberAccountRepository memberAccountRepository;
    private TermRepository termRepository;
    private TermAgreementRepository termAgreementRepository;
    private PasswordEncoder passwordEncoder;
    private EmailVerificationHasher emailVerificationHasher;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        verificationEmailRepository = mock(VerificationEmailRepository.class);
        memberAccountRepository = mock(MemberAccountRepository.class);
        termRepository = mock(TermRepository.class);
        termAgreementRepository = mock(TermAgreementRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailVerificationHasher = mock(EmailVerificationHasher.class);

        authService = new AuthService(
                memberAccountRepository,
                passwordEncoder,
                mock(LoginSessionRepository.class),
                mock(JwtProvider.class),
                mock(CookieManager.class),
                mock(JwtConfig.class),
                termRepository,
                emailVerificationHasher,
                verificationEmailRepository,
                termAgreementRepository,
                mock(FileService.class)
        );
    }

    @Test
    void signup_validRequest_savesEveryTermSelectionThenConsumesVerification() {
        EmailVerification verification = createVerification(true);
        Term requiredTerm = createTerm(1L, "1.0", true);
        Term optionalTerm = createTerm(2L, "1.0", false);

        prepareVerifiedSignup(verification, List.of(requiredTerm, optionalTerm));
        when(passwordEncoder.encode("Zipda1234!")).thenReturn("encoded-password");
        when(memberAccountRepository.save(any(MemberAccount.class))).thenAnswer(invocation -> {
            MemberAccount member = invocation.getArgument(0);
            member.setMemberId(MEMBER_ID);
            member.setCreatedAt(LocalDateTime.of(2026, 9, 7, 12, 0));
            return member;
        });

        var response = authService.signup(createRequest(true));

        assertThat(response.memberId()).isEqualTo(String.valueOf(MEMBER_ID));
        assertThat(response.email()).isEqualTo(NORMALIZED_EMAIL);
        assertThat(response.nickname()).isEqualTo("집다사용자");
        assertThat(response.role().name()).isEqualTo("USER");
        assertThat(response.status().name()).isEqualTo("ACTIVE");

        ArgumentCaptor<MemberAccount> memberCaptor = ArgumentCaptor.forClass(MemberAccount.class);
        verify(memberAccountRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(memberCaptor.getValue().getEmailVerificationAt())
                .isEqualTo(verification.getVerifiedAt());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TermAgreement>> agreementsCaptor = ArgumentCaptor.forClass(List.class);
        verify(termAgreementRepository).saveAll(agreementsCaptor.capture());
        assertThat(agreementsCaptor.getValue())
                .extracting(TermAgreement::getTermId, TermAgreement::getAgreed)
                .containsExactly(
                        tuple(1L, true),
                        tuple(2L, false)
                );
        assertThat(agreementsCaptor.getValue())
                .extracting(TermAgreement::getMemberId)
                .containsOnly(MEMBER_ID);

        verify(verificationEmailRepository).delete(verification);
    }

    @Test
    void signup_unverifiedEmail_rejectsSignup() {
        EmailVerification verification = createVerification(false);
        when(verificationEmailRepository.findByIdForUpdate(VERIFICATION_ID))
                .thenReturn(Optional.of(verification));
        when(emailVerificationHasher.hashEmail(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);

        BusinessException exception = catchThrowableOfType(
                () -> authService.signup(createRequest(true)),
                BusinessException.class
        );

        assertThat(exception.getCustomResponseCode())
                .isEqualTo(CustomResponseCode.EMAIL_VERIFICATION_REQUIRED);
        verify(memberAccountRepository, never()).save(any());
        verify(verificationEmailRepository, never()).delete(any());
    }

    @Test
    void signup_requiredTermNotAgreed_rejectsSignup() {
        EmailVerification verification = createVerification(true);
        prepareVerifiedSignup(
                verification,
                List.of(createTerm(1L, "1.0", true), createTerm(2L, "1.0", false))
        );

        BusinessException exception = catchThrowableOfType(
                () -> authService.signup(createRequest(false)),
                BusinessException.class
        );

        assertThat(exception.getCustomResponseCode())
                .isEqualTo(CustomResponseCode.INVALID_PARAMETER_ERROR);
        verify(memberAccountRepository, never()).save(any());
        verify(termAgreementRepository, never()).saveAll(any());
        verify(verificationEmailRepository, never()).delete(any());
    }

    @Test
    void signup_missingOptionalTermSelection_rejectsSignupWithoutHistoryLoss() {
        EmailVerification verification = createVerification(true);
        prepareVerifiedSignup(
                verification,
                List.of(createTerm(1L, "1.0", true), createTerm(2L, "1.0", false))
        );

        CreateMemberRequest request = createRequest(
                List.of(new TermAgreementRequest("1", "1.0", true))
        );

        BusinessException exception = catchThrowableOfType(
                () -> authService.signup(request),
                BusinessException.class
        );

        assertThat(exception.getCustomResponseCode())
                .isEqualTo(CustomResponseCode.INVALID_PARAMETER_ERROR);
        verify(memberAccountRepository, never()).save(any());
        verify(termAgreementRepository, never()).saveAll(any());
    }

    private void prepareVerifiedSignup(EmailVerification verification, List<Term> activeTerms) {
        when(verificationEmailRepository.findByIdForUpdate(VERIFICATION_ID))
                .thenReturn(Optional.of(verification));
        when(emailVerificationHasher.hashEmail(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
        when(memberAccountRepository.existsByEmail(NORMALIZED_EMAIL)).thenReturn(false);
        when(memberAccountRepository.existsByNickname("집다사용자")).thenReturn(false);
        when(termRepository.findAllByStatusTrue()).thenReturn(activeTerms);
    }

    private CreateMemberRequest createRequest(boolean requiredTermAgreed) {
        return createRequest(List.of(
                new TermAgreementRequest("1", "1.0", requiredTermAgreed),
                new TermAgreementRequest("2", "1.0", false)
        ));
    }

    private CreateMemberRequest createRequest(List<TermAgreementRequest> agreements) {
        return new CreateMemberRequest(
                EMAIL,
                String.valueOf(VERIFICATION_ID),
                "Zipda1234!",
                "Zipda1234!",
                "홍길동",
                "집다사용자",
                "01012345678",
                null,
                agreements
        );
    }

    private EmailVerification createVerification(boolean verified) {
        EmailVerification verification = new EmailVerification();
        verification.setVerificationId(VERIFICATION_ID);
        verification.setVerificationEmail(EMAIL_HASH);
        verification.setPurpose(EmailVerificationPurposePolicy.SIGNUP);
        verification.setVerificationCode("verification-code-hash");
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        if (verified) {
            verification.verify(LocalDateTime.now().minusSeconds(10));
        }
        return verification;
    }

    private Term createTerm(Long id, String version, boolean required) {
        Term term = new Term();
        term.setTermId(id);
        term.setTermVersion(version);
        term.setIsRequired(required);
        term.setStatus(true);
        return term;
    }
}
