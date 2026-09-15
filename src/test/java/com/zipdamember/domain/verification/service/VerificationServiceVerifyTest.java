package com.zipdamember.domain.verification.service;

import com.zipdamember.domain.verification.constant.EmailVerificationPurposePolicy;
import com.zipdamember.domain.verification.entity.EmailVerification;
import com.zipdamember.domain.verification.repository.VerificationEmailRepository;
import com.zipdamember.domain.verification.repository.VerificationMemberRepository;
import com.zipdamember.domain.verification.request.VerifyEmailVerificationRequest;
import com.zipdamember.domain.verification.util.EmailVerificationHasher;
import com.zipdamember.domain.verification.util.VerificationCodeGenerator;
import com.zipdamember.domain.verification.validator.MemberFormatValidator;
import com.zipdamember.global.error.custom.business.VerificationAttemptException;
import com.zipdamember.global.mail.EmailSender;
import com.zipdamember.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationServiceVerifyTest {

    private static final Long VERIFICATION_ID = 1L;
    private static final String EMAIL = "user@gmail.com";
    private static final String EMAIL_HASH = "email-hash";
    private static final String RAW_CODE = "381920";
    private static final String CODE_HASH = "code-hash";

    private VerificationEmailRepository verificationEmailRepository;
    private EmailVerificationHasher emailVerificationHasher;
    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        MemberFormatValidator memberFormatValidator = mock(
                MemberFormatValidator.class
        );
        VerificationMemberRepository verificationMemberRepository = mock(
                VerificationMemberRepository.class
        );
        verificationEmailRepository = mock(
                VerificationEmailRepository.class
        );
        VerificationCodeGenerator verificationCodeGenerator = mock(
                VerificationCodeGenerator.class
        );
        emailVerificationHasher = mock(EmailVerificationHasher.class);
        EmailSender emailSender = mock(EmailSender.class);

        verificationService = new VerificationService(
                memberFormatValidator,
                verificationMemberRepository,
                verificationEmailRepository,
                verificationCodeGenerator,
                emailVerificationHasher,
                emailSender
        );
    }

    @Test
    void verifyVerificationCode_matchingCode_marksVerificationAsVerified() {
        EmailVerification emailVerification = createActiveVerification();
        when(verificationEmailRepository.findByIdForUpdate(VERIFICATION_ID))
                .thenReturn(Optional.of(emailVerification));
        when(emailVerificationHasher.hashEmail(EMAIL))
                .thenReturn(EMAIL_HASH);
        when(emailVerificationHasher.matches(RAW_CODE, CODE_HASH))
                .thenReturn(true);

        var response = verificationService.verifyVerificationCode(
                VERIFICATION_ID,
                new VerifyEmailVerificationRequest(EMAIL, RAW_CODE),
                EmailVerificationPurposePolicy.SIGNUP
        );

        assertThat(response.verificationId()).isEqualTo(String.valueOf(VERIFICATION_ID));
        assertThat(response.verified()).isTrue();
        assertThat(response.verifiedAt()).isNotNull();
        assertThat(emailVerification.isVerified()).isTrue();
        verify(verificationEmailRepository).findByIdForUpdate(
                VERIFICATION_ID
        );
    }

    @Test
    void verifyVerificationCode_wrongCode_increasesAttemptCount() {
        EmailVerification emailVerification = createActiveVerification();
        when(verificationEmailRepository.findByIdForUpdate(VERIFICATION_ID))
                .thenReturn(Optional.of(emailVerification));
        when(emailVerificationHasher.hashEmail(EMAIL))
                .thenReturn(EMAIL_HASH);
        when(emailVerificationHasher.matches(RAW_CODE, CODE_HASH))
                .thenReturn(false);

        VerificationAttemptException exception = catchThrowableOfType(
                () -> verificationService.verifyVerificationCode(
                        VERIFICATION_ID,
                        new VerifyEmailVerificationRequest(EMAIL, RAW_CODE),
                        EmailVerificationPurposePolicy.SIGNUP
                ),
                VerificationAttemptException.class
        );

        assertThat(exception.getCustomResponseCode())
                .isEqualTo(CustomResponseCode.VERIFICATION_CODE_INVALID);
        assertThat(emailVerification.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void verifyVerificationCode_fifthFailure_returnsAttemptsExceeded() {
        EmailVerification emailVerification = createActiveVerification();
        emailVerification.setAttemptCount(4);
        when(verificationEmailRepository.findByIdForUpdate(VERIFICATION_ID))
                .thenReturn(Optional.of(emailVerification));
        when(emailVerificationHasher.hashEmail(EMAIL))
                .thenReturn(EMAIL_HASH);
        when(emailVerificationHasher.matches(RAW_CODE, CODE_HASH))
                .thenReturn(false);

        VerificationAttemptException exception = catchThrowableOfType(
                () -> verificationService.verifyVerificationCode(
                        VERIFICATION_ID,
                        new VerifyEmailVerificationRequest(EMAIL, RAW_CODE),
                        EmailVerificationPurposePolicy.SIGNUP
                ),
                VerificationAttemptException.class
        );

        assertThat(exception.getCustomResponseCode())
                .isEqualTo(
                        CustomResponseCode.VERIFICATION_ATTEMPTS_EXCEEDED
                );
        assertThat(emailVerification.getAttemptCount()).isEqualTo(5);
    }

    private EmailVerification createActiveVerification() {
        EmailVerification emailVerification = new EmailVerification();
        emailVerification.setVerificationEmail(EMAIL_HASH);
        emailVerification.setPurpose(EmailVerificationPurposePolicy.SIGNUP);
        emailVerification.setVerificationCode(CODE_HASH);
        emailVerification.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        emailVerification.setVerificationId(VERIFICATION_ID);

        return emailVerification;
    }
}
