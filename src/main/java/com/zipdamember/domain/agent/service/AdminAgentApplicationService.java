package com.zipdamember.domain.agent.service;

import com.zipdamember.domain.admin.constant.AdminAuditAction;
import com.zipdamember.domain.admin.constant.AdminAuditActorType;
import com.zipdamember.domain.admin.constant.AdminAuditTargetService;
import com.zipdamember.domain.admin.constant.AdminAuditValueType;
import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.admin.request.AdminAuditLogWriteRequest;
import com.zipdamember.domain.admin.service.AdminAuditLogService;
import com.zipdamember.domain.agent.client.MolitAgencyRegistrationClient;
import com.zipdamember.domain.agent.client.NtsBusinessValidationClient;
import com.zipdamember.domain.agent.constant.AgentApplicationStatus;
import com.zipdamember.domain.agent.constant.AgentApplicationDocumentType;
import com.zipdamember.domain.agent.constant.VerificationResultStatus;
import com.zipdamember.domain.agent.entity.AgentApplication;
import com.zipdamember.domain.agent.entity.AgentProfile;
import com.zipdamember.domain.agent.entity.AgencyRegistrationVerification;
import com.zipdamember.domain.agent.entity.BusinessVerification;
import com.zipdamember.domain.agent.repository.AgencyRegistrationVerificationRepository;
import com.zipdamember.domain.agent.repository.AgentApplicationDocumentRepository;
import com.zipdamember.domain.agent.repository.AgentApplicationRepository;
import com.zipdamember.domain.agent.repository.AgentProfileRepository;
import com.zipdamember.domain.agent.repository.AdminAgentApplicationQueryRepository;
import com.zipdamember.domain.agent.repository.BusinessVerificationRepository;
import com.zipdamember.domain.agent.request.AdminAgentApplicationSearchRequest;
import com.zipdamember.domain.agent.request.AdminAgentApplicationApproveRequest;
import com.zipdamember.domain.agent.request.AdminAgentApplicationSupplementRequest;
import com.zipdamember.domain.agent.response.AdminAgencyRegistrationVerificationResponse;
import com.zipdamember.domain.agent.response.AdminAgentApplicationApproveResponse;
import com.zipdamember.domain.agent.response.AdminAgentApplicationDetailResponse;
import com.zipdamember.domain.agent.response.AdminAgentApplicationDocumentResponse;
import com.zipdamember.domain.agent.response.AdminAgentApplicationListResponse;
import com.zipdamember.domain.agent.response.AdminAgentApplicationSupplementResponse;
import com.zipdamember.domain.agent.response.AdminBusinessVerificationResponse;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.domain.file.service.FileService;
import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.global.security.constant.MemberRolePolicy;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.error.custom.business.NotFoundResourceException;
import com.zipdamember.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAgentApplicationService {

    private final AdminAgentApplicationQueryRepository adminAgentApplicationQueryRepository;
    private final AgentApplicationRepository agentApplicationRepository;
    private final AgentApplicationDocumentRepository agentApplicationDocumentRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final BusinessVerificationRepository businessVerificationRepository;
    private final AgencyRegistrationVerificationRepository agencyRegistrationVerificationRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final AdminAuditLogService adminAuditLogService;
    private final NtsBusinessValidationClient ntsBusinessValidationClient;
    private final MolitAgencyRegistrationClient molitAgencyRegistrationClient;
    private final FileService fileService;

    @Transactional(readOnly = true)
    public AdminAgentApplicationListResponse search(AdminAgentApplicationSearchRequest request) {
        // 심사 대기 목록 조회
        return AdminAgentApplicationListResponse.from(
                adminAgentApplicationQueryRepository.search(
                        request.applicant(),
                        request.agencyName(),
                        request.businessRegistrationNo(),
                        request.status(),
                        PageRequest.of(request.page(), request.size())
                )
        );
    }

    @Transactional(readOnly = true)
    public AdminAgentApplicationDetailResponse getDetail(Long applicationId) {
        // 중개사 신청 조회
        AgentApplication application = agentApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundResourceException("중개사 신청 정보를 찾을 수 없습니다."));

        // 신청자 조회
        String applicantName = memberAccountRepository.findById(application.getMemberId())
                .map(member -> member.getName())
                .orElse(null);

        // 최신 사업자 검증 결과 조회
        AdminBusinessVerificationResponse businessVerification = businessVerificationRepository
                .findTopByApplicationIdOrderByVerificationIdDesc(applicationId)
                .map(AdminBusinessVerificationResponse::from)
                .orElse(null);

        // 최신 등록번호 검증 결과 조회
        AdminAgencyRegistrationVerificationResponse agencyRegistrationVerification =
                agencyRegistrationVerificationRepository
                        .findTopByApplicationIdOrderByVerificationIdDesc(applicationId)
                        .map(AdminAgencyRegistrationVerificationResponse::from)
                        .orElse(null);

        // 신청 서류 목록 조회
        var documents = agentApplicationDocumentRepository
                .findAllByApplicationIdOrderByUploadedAtDescDocumentIdDesc(applicationId)
                .stream()
                .map(document -> AdminAgentApplicationDocumentResponse.of(
                        document,
                        fileService.createPrivateDownloadUrl(document.getFileId())
                ))
                .toList();

        // 신청 상세 응답
        return AdminAgentApplicationDetailResponse.of(
                application,
                applicantName,
                businessVerification,
                agencyRegistrationVerification,
                documents
        );
    }

    @Transactional
    public AdminAgentApplicationSupplementResponse requestSupplement(
            Long applicationId,
            AdminAgentApplicationSupplementRequest request,
            Long reviewerAdminId,
            AdminRoleCode reviewerRole,
            String ipAddress,
            String userAgent
    ) {
        // 중개사 신청 조회
        AgentApplication application = agentApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundResourceException("중개사 신청 정보를 찾을 수 없습니다."));

        // 심사 상태 검증
        if (application.getStatus() != AgentApplicationStatus.UNDER_REVIEW) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "심사 중 상태의 신청만 보완 요청할 수 있습니다."
            );
        }

        // 보완 요청 상태 변경
        application.requestSupplement(
                request.supplementReason(),
                request.supplementDeadline(),
                reviewerAdminId
        );

        // 보완 요청 감사 로그
        adminAuditLogService.recordSuccess(new AdminAuditLogWriteRequest(
                reviewerAdminId,
                AdminAuditActorType.ADMIN,
                reviewerRole,
                AdminAuditAction.AGENT_APPLICATION_SUPPLEMENT_REQUEST,
                AdminAuditTargetService.MEMBER,
                "AGENT_APPLICATION",
                application.getApplicationId().toString(),
                request.supplementReason(),
                ipAddress,
                userAgent,
                List.of(
                        new AdminAuditLogWriteRequest.Change(
                                "status",
                                AgentApplicationStatus.UNDER_REVIEW.name(),
                                application.getStatus().name(),
                                AdminAuditValueType.ENUM,
                                0
                        ),
                        new AdminAuditLogWriteRequest.Change(
                                "supplementDeadline",
                                null,
                                request.supplementDeadline().toString(),
                                AdminAuditValueType.DATETIME,
                                1
                        )
                )
        ));

        // 보완 요청 응답
        return AdminAgentApplicationSupplementResponse.from(application);
    }

    @Transactional
    public AdminAgentApplicationApproveResponse approve(
            Long applicationId,
            AdminAgentApplicationApproveRequest request,
            Long reviewerAdminId,
            AdminRoleCode reviewerRole,
            String ipAddress,
            String userAgent
    ) {
        // 중개사 신청 잠금 조회
        AgentApplication application = agentApplicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new NotFoundResourceException("중개사 신청 정보를 찾을 수 없습니다."));

        // 심사 상태 검증
        if (application.getStatus() != AgentApplicationStatus.UNDER_REVIEW) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "심사 중 상태의 신청만 승인할 수 있습니다."
            );
        }

        // 필수 서류 존재 검증
        validateRequiredDocuments(applicationId);

        // 최신 API 검증 결과 확인
        var businessVerification = businessVerificationRepository
                .findTopByApplicationIdOrderByVerificationIdDesc(applicationId)
                .orElseThrow(() -> invalidApproval("사업자등록번호 검증 이력이 없습니다."));
        var agencyRegistrationVerification = agencyRegistrationVerificationRepository
                .findTopByApplicationIdOrderByVerificationIdDesc(applicationId)
                .orElseThrow(() -> invalidApproval("공인중개사등록번호 검증 이력이 없습니다."));
        validateMatchedVerifications(businessVerification.getResultStatus(), agencyRegistrationVerification.getResultStatus());

        // 신청 회원 조회
        var member = memberAccountRepository.findById(application.getMemberId())
                .orElseThrow(() -> new NotFoundResourceException("신청 회원 정보를 찾을 수 없습니다."));

        // 신청 회원 상태 검증
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw invalidApproval("활성 상태의 회원만 중개사로 승인할 수 있습니다.");
        }

        // 사업자등록번호 중복 검증
        if (agentProfileRepository.existsByBusinessRegistrationNo(application.getRequestBusinessNo())) {
            throw invalidApproval("이미 등록된 사업자등록번호입니다.");
        }

        // 중개사 신청 승인 처리
        application.approve(reviewerAdminId);

        // 중개소 주소 결정
        String agencyAddress = resolveAddress(
                agencyRegistrationVerification.getRoadAddress(),
                agencyRegistrationVerification.getJibunAddress()
        );

        // 중개사 프로필 등록
        AgentProfile agentProfile = agentProfileRepository.save(new AgentProfile(
                application.getMemberId(),
                application.getRequestBusinessNo(),
                application.getRequestAgencyName(),
                application.getRequestRepresentativeName(),
                member.getPhone(),
                agencyAddress,
                LocalDateTime.now()
        ));

        // 회원 중개사 역할 활성화
        MemberRolePolicy previousRole = member.getMemberRole();
        member.activateAgentRole();

        // 승인 감사 로그
        adminAuditLogService.recordSuccess(new AdminAuditLogWriteRequest(
                reviewerAdminId,
                AdminAuditActorType.ADMIN,
                reviewerRole,
                AdminAuditAction.AGENT_APPLICATION_APPROVE,
                AdminAuditTargetService.MEMBER,
                "AGENT_APPLICATION",
                application.getApplicationId().toString(),
                resolveApprovalReason(request.reviewNote()),
                ipAddress,
                userAgent,
                List.of(
                        new AdminAuditLogWriteRequest.Change(
                                "status",
                                AgentApplicationStatus.UNDER_REVIEW.name(),
                                application.getStatus().name(),
                                AdminAuditValueType.ENUM,
                                0
                        ),
                        new AdminAuditLogWriteRequest.Change(
                                "memberRole",
                                previousRole.name(),
                                member.getMemberRole().name(),
                                AdminAuditValueType.ENUM,
                                1
                        ),
                        new AdminAuditLogWriteRequest.Change(
                                "agentId",
                                null,
                                agentProfile.getAgentId().toString(),
                                AdminAuditValueType.NUMBER,
                                2
                        )
                )
        ));

        // 승인 응답
        return AdminAgentApplicationApproveResponse.of(application, agentProfile);
    }

    private void validateRequiredDocuments(Long applicationId) {
        // 사업자등록증 존재 검증
        if (!agentApplicationDocumentRepository.existsByApplicationIdAndDocumentType(
                applicationId,
                AgentApplicationDocumentType.BUSINESS_LICENSE
        )) {
            throw invalidApproval("사업자등록증이 등록되지 않았습니다.");
        }

        // 공인중개사무소 등록증 존재 검증
        if (!agentApplicationDocumentRepository.existsByApplicationIdAndDocumentType(
                applicationId,
                AgentApplicationDocumentType.BROKER_OFFICE_LICENSE
        )) {
            throw invalidApproval("공인중개사무소 등록증이 등록되지 않았습니다.");
        }
    }

    private void validateMatchedVerifications(
            VerificationResultStatus businessVerificationResult,
            VerificationResultStatus agencyRegistrationVerificationResult
    ) {
        // 사업자등록번호 검증 결과 확인
        if (businessVerificationResult != VerificationResultStatus.MATCHED) {
            throw invalidApproval("사업자등록번호 검증 결과가 일치하지 않습니다.");
        }

        // 공인중개사등록번호 검증 결과 확인
        if (agencyRegistrationVerificationResult != VerificationResultStatus.MATCHED) {
            throw invalidApproval("공인중개사등록번호 검증 결과가 일치하지 않습니다.");
        }
    }

    private String resolveAddress(String roadAddress, String jibunAddress) {
        // 중개소 주소 결정
        return roadAddress == null || roadAddress.isBlank() ? jibunAddress : roadAddress;
    }

    private String resolveApprovalReason(String reviewNote) {
        // 승인 감사 사유 결정
        return reviewNote == null || reviewNote.isBlank() ? "중개사 신청 승인" : reviewNote;
    }

    private BusinessException invalidApproval(String message) {
        // 승인 요청 오류 생성
        return new BusinessException(CustomResponseCode.INVALID_PARAMETER_ERROR, message);
    }

    @Transactional
    public void verifyPendingApplications() {
        // 외부 API 설정 검증
        if (!ntsBusinessValidationClient.isConfigured() || !molitAgencyRegistrationClient.isConfigured()) {
            return;
        }

        // 대기 신청 검증 대상 조회
        List<AgentApplication> applications = agentApplicationRepository
                .findTop100ByStatusAndSubmittedAtIsNotNullOrderBySubmittedAtAscApplicationIdAsc(AgentApplicationStatus.PENDING);

        // 대기 신청 검증 처리
        for (AgentApplication application : applications) {
            verifyPendingApplication(application);
        }
    }

    private void verifyPendingApplication(AgentApplication application) {
        // 국세청 사업자 검증 호출
        var businessResult = ntsBusinessValidationClient.validate(application);

        // 국토교통부 등록번호 검증 호출
        var agencyRegistrationResult = molitAgencyRegistrationClient.validate(application);

        // 검증 이력 확인 시각 생성
        LocalDateTime checkedAt = LocalDateTime.now();

        // 사업자 검증 이력 저장
        businessVerificationRepository.save(BusinessVerification.create(
                application.getApplicationId(),
                application.getRequestBusinessNo(),
                application.getRequestStartDate(),
                application.getRequestRepresentativeName(),
                businessResult.resultStatus(),
                checkedAt
        ));

        // 등록번호 검증 이력 저장
        agencyRegistrationVerificationRepository.save(AgencyRegistrationVerification.create(
                application.getApplicationId(),
                application.getRequestAgencyRegistrationNo(),
                application.getRequestAgencyName(),
                application.getRequestRepresentativeName(),
                agencyRegistrationResult.resultStatus(),
                agencyRegistrationResult.businessStatus(),
                agencyRegistrationResult.roadAddress(),
                agencyRegistrationResult.jibunAddress(),
                checkedAt
        ));

        // 신청 상태 변경 전 값 보관
        AgentApplicationStatus previousStatus = application.getStatus();

        // 신청 상태 검증 결과 반영
        AgentApplicationStatus changedStatus = application.applyVerificationResults(
                businessResult.resultStatus(),
                agencyRegistrationResult.resultStatus()
        );

        // 상태 변경 감사 로그 저장
        if (previousStatus != changedStatus) {
            recordVerificationStateChange(application, previousStatus, changedStatus);
        }
    }

    private void recordVerificationStateChange(
            AgentApplication application,
            AgentApplicationStatus previousStatus,
            AgentApplicationStatus changedStatus
    ) {
        // 검증 결과 감사 로그 저장
        adminAuditLogService.recordSuccess(new AdminAuditLogWriteRequest(
                null,
                AdminAuditActorType.SYSTEM,
                null,
                resolveVerificationAction(changedStatus),
                AdminAuditTargetService.MEMBER,
                "AGENT_APPLICATION",
                application.getApplicationId().toString(),
                resolveVerificationReason(changedStatus),
                null,
                null,
                List.of(new AdminAuditLogWriteRequest.Change(
                        "status",
                        previousStatus.name(),
                        changedStatus.name(),
                        AdminAuditValueType.ENUM,
                        0
                ))
        ));
    }

    private AdminAuditAction resolveVerificationAction(AgentApplicationStatus changedStatus) {
        // 검증 상태 감사 행위 결정
        return changedStatus == AgentApplicationStatus.UNDER_REVIEW
                ? AdminAuditAction.AGENT_APPLICATION_UNDER_REVIEW
                : AdminAuditAction.AGENT_APPLICATION_INCORRECT_DATA;
    }

    private String resolveVerificationReason(AgentApplicationStatus changedStatus) {
        // 검증 상태 감사 사유 결정
        return changedStatus == AgentApplicationStatus.UNDER_REVIEW
                ? "국세청·국토교통부 API 검증 일치"
                : "국세청 또는 국토교통부 API 검증 불일치";
    }
}
