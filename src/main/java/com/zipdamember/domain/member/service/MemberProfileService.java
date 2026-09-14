package com.zipdamember.domain.member.service;

import com.zipdamember.domain.agent.repository.AgentProfileRepository;
import com.zipdamember.domain.auth.repository.LoginSessionRepository;
import com.zipdamember.domain.file.constant.FileCategory;
import com.zipdamember.domain.file.service.FileService;
import com.zipdamember.domain.member.constant.ProfileImageUpdatePolicy;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.domain.member.event.MemberProfileUpdatedEvent;
import com.zipdamember.domain.member.event.PasswordChangedEvent;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.domain.member.request.MemberEmailVerificationRequest;
import com.zipdamember.domain.member.request.MemberPasswordChangeRequest;
import com.zipdamember.domain.member.request.MemberProfileUpdateRequest;
import com.zipdamember.domain.member.response.MemberAgentSummaryResponse;
import com.zipdamember.domain.member.response.MemberProfileResponse;
import com.zipdamember.domain.verification.constant.EmailVerificationPurposePolicy;
import com.zipdamember.domain.verification.entity.EmailVerification;
import com.zipdamember.domain.verification.repository.VerificationEmailRepository;
import com.zipdamember.domain.verification.request.EmailVerificationRequest;
import com.zipdamember.domain.verification.request.VerifyEmailVerificationRequest;
import com.zipdamember.domain.verification.response.EmailVerificationResponse;
import com.zipdamember.domain.verification.response.VerifyEmailVerificationResponse;
import com.zipdamember.domain.verification.service.VerificationService;
import com.zipdamember.domain.verification.util.EmailVerificationHasher;
import com.zipdamember.domain.verification.util.VerificationIdParser;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberProfileService {
    private final MemberAccountRepository memberAccountRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final VerificationEmailRepository verificationEmailRepository;
    private final VerificationService verificationService;
    private final EmailVerificationHasher emailVerificationHasher;
    private final LoginSessionRepository loginSessionRepository;
    private final FileService fileService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public MemberProfileResponse getMyProfile(Long memberId) {
        MemberAccount member = getMember(memberId);
        MemberAgentSummaryResponse agent = agentProfileRepository.findByMemberId(memberId)
                .map(MemberAgentSummaryResponse::from)
                .orElse(null);
        return toResponse(member, agent);
    }

    @Transactional
    public MemberProfileResponse updateMyProfile(Long memberId, MemberProfileUpdateRequest request) {
        MemberAccount member = getMember(memberId);
        String nickname = normalizeNullable(request.nickname());
        String phone = normalizeNullable(request.phone());
        ProfileImageUpdatePolicy imageAction = request.profileImageAction() == null
                ? ProfileImageUpdatePolicy.KEEP
                : request.profileImageAction();

        if (nickname == null && phone == null && imageAction == ProfileImageUpdatePolicy.KEEP) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "변경할 프로필 정보를 한 개 이상 전달해야 합니다."
            );
        }
        if (nickname != null
                && !nickname.equals(member.getNickname())
                && memberAccountRepository.existsByNickname(nickname)) {
            throw new BusinessException(
                    CustomResponseCode.DUPLICATED_RESOURCE_ERROR,
                    "이미 사용 중인 닉네임입니다."
            );
        }

        Long profileFileId = resolveProfileFileId(member, request, imageAction);
        member.updateProfile(nickname, phone, profileFileId);
        eventPublisher.publishEvent(new MemberProfileUpdatedEvent(
                memberId.toString(),
                member.getNickname(),
                profileFileId == null ? null : profileFileId.toString()
        ));
        return toResponse(
                member,
                agentProfileRepository.findByMemberId(memberId)
                        .map(MemberAgentSummaryResponse::from)
                        .orElse(null)
        );
    }

    public EmailVerificationResponse sendPasswordVerification(Long memberId) {
        MemberAccount member = getMember(memberId);
        return verificationService.sendVerificationCode(
                new EmailVerificationRequest(member.getEmail()),
                EmailVerificationPurposePolicy.PASSWORD_RESET
        );
    }

    public VerifyEmailVerificationResponse verifyPasswordCode(
            Long memberId,
            Long verificationId,
            MemberEmailVerificationRequest request
    ) {
        MemberAccount member = getMember(memberId);
        return verificationService.verifyVerificationCode(
                verificationId,
                new VerifyEmailVerificationRequest(member.getEmail(), request.verificationCode()),
                EmailVerificationPurposePolicy.PASSWORD_RESET
        );
    }

    @Transactional
    public void changePassword(Long memberId, MemberPasswordChangeRequest request) {
        MemberAccount member = getMember(memberId);
        if (member.getPassword() == null
                || !passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "현재 비밀번호가 일치하지 않습니다."
            );
        }
        if (!request.newPassword().equals(request.newPasswordCheck())) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }

        EmailVerification verification = validateCompletedVerification(
                VerificationIdParser.parse(request.verificationId()),
                member.getEmail()
        );
        member.changePassword(passwordEncoder.encode(request.newPassword()));
        loginSessionRepository.findAllByMemberIdAndRevokedAtIsNull(memberId)
                .forEach(session -> session.revoke());
        verificationEmailRepository.delete(verification);
        eventPublisher.publishEvent(new PasswordChangedEvent(memberId.toString()));
    }

    private EmailVerification validateCompletedVerification(Long verificationId, String email) {
        EmailVerification verification = verificationEmailRepository.findByIdForUpdate(verificationId)
                .orElseThrow(() -> new BusinessException(
                        CustomResponseCode.EMAIL_VERIFICATION_NOT_FOUND,
                        "이메일 인증 정보를 찾을 수 없습니다."
                ));
        if (verification.getPurpose() != EmailVerificationPurposePolicy.PASSWORD_RESET
                || !verification.isVerified()) {
            throw new BusinessException(
                    CustomResponseCode.EMAIL_VERIFICATION_REQUIRED,
                    "비밀번호 변경용 이메일 인증을 완료해야 합니다."
            );
        }
        if (!verification.getVerificationEmail().equals(emailVerificationHasher.hashEmail(email))) {
            throw new BusinessException(
                    CustomResponseCode.EMAIL_VERIFICATION_EMAIL_MISMATCH,
                    "인증을 완료한 이메일과 회원 이메일이 일치하지 않습니다."
            );
        }
        return verification;
    }

    private Long resolveProfileFileId(
            MemberAccount member,
            MemberProfileUpdateRequest request,
            ProfileImageUpdatePolicy action
    ) {
        if (action == ProfileImageUpdatePolicy.KEEP) return member.getProfileFileId();
        if (action == ProfileImageUpdatePolicy.REMOVE) return null;
        if (request.profileFileId() == null || request.profileFileId().isBlank()) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "교체할 프로필 파일 식별자는 필수입니다."
            );
        }
        Long fileId = parseId(request.profileFileId(), "프로필 파일");
        fileService.validateOwnedProfile(fileId, member.getMemberId(), FileCategory.PROFILE);
        return fileId;
    }

    private MemberProfileResponse toResponse(
            MemberAccount member,
            MemberAgentSummaryResponse agent
    ) {
        return MemberProfileResponse.from(
                member,
                fileService.getPublicFileUri(member.getProfileFileId()),
                agent
        );
    }

    private MemberAccount getMember(Long memberId) {
        return memberAccountRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        CustomResponseCode.NOT_FOUND_RESOURCE_ERROR,
                        "회원 정보를 찾을 수 없습니다."
                ));
    }

    private String normalizeNullable(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Long parseId(String value, String field) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "올바르지 않은 " + field + " 식별자입니다."
            );
        }
    }
}
