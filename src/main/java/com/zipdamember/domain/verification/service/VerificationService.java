package com.zipdamember.domain.verification.service;

import com.zipdamember.domain.verification.constant.EmailVerificationPurposePolicy;
import com.zipdamember.domain.verification.constant.MemberValidationTypePolicy;
import com.zipdamember.domain.verification.entity.EmailVerification;
import com.zipdamember.domain.verification.repository.VerificationEmailRepository;
import com.zipdamember.domain.verification.repository.VerificationMemberRepository;
import com.zipdamember.domain.verification.request.EmailVerificationRequest;
import com.zipdamember.domain.verification.request.ValidateMemberRequest;
import com.zipdamember.domain.verification.request.VerifyEmailVerificationRequest;
import com.zipdamember.domain.verification.response.EmailVerificationResponse;
import com.zipdamember.domain.verification.response.RegistrationDuplicateResponse;
import com.zipdamember.domain.verification.response.VerifyEmailVerificationResponse;
import com.zipdamember.domain.verification.util.EmailVerificationHasher;
import com.zipdamember.domain.verification.util.VerificationCodeGenerator;
import com.zipdamember.domain.verification.validator.MemberFormatValidator;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.error.custom.business.AlreadyRegisteredException;
import com.zipdamember.global.error.custom.business.VerificationAttemptException;
import com.zipdamember.global.mail.EmailSender;
import com.zipdamember.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    // 이메일 인증번호 유효 시간(분)
    private static final int VERIFICATION_EXPIRY_MINUTES = 5;
    // 이메일 인증번호 유효 시간(초)
    private static final int VERIFICATION_EXPIRY_SECONDS = 300;
    // 같은 이메일로 인증번호를 다시 요청할려면 60초 기다려야함
    private static final int RESEND_LIMIT_SECONDS = 60;
    // 이메일 인증 최대 실패 횟수. 정책 확정 시 이 값만 변경한다.
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;

    private final MemberFormatValidator memberFormatValidator;
    private final VerificationMemberRepository verificationMemberRepository;
    private final VerificationEmailRepository verificationEmailRepository;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final EmailVerificationHasher emailVerificationHasher;
    private final EmailSender emailSender;

    /**
     * 이미 가입한 nickname or email인지 검사
     * @param validateMemberRequest
     * @return RegistrationDuplicateResponse
     */
    public RegistrationDuplicateResponse checkDuplicate(ValidateMemberRequest validateMemberRequest) {
        memberFormatValidator.validate(validateMemberRequest);
        if (validateMemberRequest.typePolicy() == MemberValidationTypePolicy.EMAIL) {
            boolean isExist = verificationMemberRepository.existsByEmail(validateMemberRequest.value());
            return RegistrationDuplicateResponse.from(!isExist, validateMemberRequest.typePolicy());
        } else if (validateMemberRequest.typePolicy() == MemberValidationTypePolicy.NICKNAME) {
            boolean isExist = verificationMemberRepository.existsByNickname(validateMemberRequest.value());
            return RegistrationDuplicateResponse.from(!isExist, validateMemberRequest.typePolicy());
        } else {
            throw new BusinessException(CustomResponseCode.INVALID_PARAMETER_ERROR);
        }
    }

    /**
     * Request를 받아 전체 발송 작업 수행 후, 성공 시 Response를 반환
     * @param request
     * @return EmailVerificationResponse
     */
    public EmailVerificationResponse sendVerificationCode(EmailVerificationRequest request) {
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (verificationMemberRepository.existsByEmail(email)) {
            throw new AlreadyRegisteredException(
                    "이미 가입된 이메일입니다."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        String emailHash = emailVerificationHasher.hashEmail(email);
        LocalDateTime resendBoundary = now.minusSeconds(RESEND_LIMIT_SECONDS); // 최근 60초 안에 보낸 인증 메일 있음?

        boolean recentlySent = verificationEmailRepository
                // 인증 이메일이 일치하고 인증 목적이 일치하며 만료 시각이 기준 시각보다 뒤인 데이터가 존재?
                .existsByVerificationEmailAndPurposeAndCreatedAtAfter(
                        emailHash,
                        EmailVerificationPurposePolicy.SIGNUP,
                        resendBoundary
                );

        if (recentlySent) {
            throw new BusinessException(
                    CustomResponseCode.EMAIL_RESEND_LIMIT_ERROR,
                    "인증번호는 60초 후 다시 요청할 수 있습니다."
            );
        }

        String verificationCode = verificationCodeGenerator.generate();
        String verificationCodeHash = emailVerificationHasher.hashVerificationCode(verificationCode);

        EmailVerification emailVerification = EmailVerification.create(
                emailHash,
                EmailVerificationPurposePolicy.SIGNUP,
                verificationCodeHash,
                now.plusMinutes(VERIFICATION_EXPIRY_MINUTES)
        );

        EmailVerification savedEmailVerification = verificationEmailRepository.save(emailVerification);

        try {
            emailSender.sendVerificationCode(email, verificationCode);
        } catch (BusinessException exception) {
            verificationEmailRepository.deleteById(savedEmailVerification.getVerificationId());
            throw exception;
        }

        return EmailVerificationResponse.from(savedEmailVerification, VERIFICATION_EXPIRY_SECONDS, RESEND_LIMIT_SECONDS);
    }

    /**
     * 사용자가 입력한 이메일 인증번호를 확인한다.
     * 인증번호 불일치 시 attemptCount 증가를 보존하기 위해
     * VerificationAttemptException에는 트랜잭션을 롤백하지 않는다.
     */
    @Transactional(noRollbackFor = VerificationAttemptException.class)
    public VerifyEmailVerificationResponse verifyVerificationCode(Long verificationId, VerifyEmailVerificationRequest request) {
        if (verificationId == null || verificationId <= 0) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "올바르지 않은 이메일 인증 식별자입니다."
            );
        }

        // findByIdForUpdate: Id로 행을 조회하고 조회한 행을 내가 곧 수정할테니 이 행 동시 수정을 잠그겠다
        EmailVerification emailVerification = verificationEmailRepository.findByIdForUpdate(verificationId)
                .orElseThrow(() -> new BusinessException(CustomResponseCode.EMAIL_VERIFICATION_NOT_FOUND, "이메일 인증 정보를 찾을 수 없습니다."));

        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);
        String emailHash = emailVerificationHasher.hashEmail(normalizedEmail);
        LocalDateTime now = LocalDateTime.now();

        if (emailVerification.getPurpose() != EmailVerificationPurposePolicy.SIGNUP) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "회원가입용 이메일 인증 정보가 아닙니다."
            );
        }

        if (!emailVerification.getVerificationEmail().equals(emailHash)) {
            throw new BusinessException(
                    CustomResponseCode.EMAIL_VERIFICATION_EMAIL_MISMATCH,
                    "인증번호를 요청한 이메일과 일치하지 않습니다."
            );
        }

        if (emailVerification.isExpired(now)) {
            throw new BusinessException(
                    CustomResponseCode.EMAIL_VERIFICATION_EXPIRED,
                    "이메일 인증번호가 만료되었습니다."
            );
        }

        if (emailVerification.isVerified()) {
            throw new BusinessException(
                    CustomResponseCode.EMAIL_VERIFICATION_ALREADY_COMPLETED,
                    "이미 완료된 이메일 인증입니다."
            );
        }

        if (emailVerification.hasReachedAttemptLimit(
                MAX_VERIFICATION_ATTEMPTS
        )) {
            throw new BusinessException(
                    CustomResponseCode.VERIFICATION_ATTEMPTS_EXCEEDED,
                    "이메일 인증번호 입력 가능 횟수를 초과했습니다."
            );
        }

        boolean matches = emailVerificationHasher.matches(
                request.verificationCode(),
                emailVerification.getVerificationCode()
        );

        if (!matches) {
            emailVerification.increaseAttemptCount();
            log.warn(
                    "이메일 인증번호 불일치: verificationId={}, attemptCount={}",
                    verificationId,
                    emailVerification.getAttemptCount()
            );

            if (emailVerification.hasReachedAttemptLimit(
                    MAX_VERIFICATION_ATTEMPTS
            )) {
                throw new VerificationAttemptException(
                        CustomResponseCode.VERIFICATION_ATTEMPTS_EXCEEDED,
                        "이메일 인증번호 입력 가능 횟수를 초과했습니다."
                );
            }

            throw new VerificationAttemptException(
                    CustomResponseCode.VERIFICATION_CODE_INVALID,
                    "이메일 인증번호가 일치하지 않습니다."
            );
        }

        emailVerification.verify(now);

        return VerifyEmailVerificationResponse.from(emailVerification);
    }

    @Transactional
    public long deleteExpiredVerifications(LocalDateTime cutoff) {
        return verificationEmailRepository.deleteExpiredBefore(cutoff);
    }
}
