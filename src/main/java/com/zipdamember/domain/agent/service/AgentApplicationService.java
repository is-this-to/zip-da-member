package com.zipdamember.domain.agent.service;

import com.zipdamember.domain.agent.constant.AgentApplicationDocumentType;
import com.zipdamember.domain.agent.constant.AgentApplicationStatus;
import com.zipdamember.domain.agent.entity.AgentApplication;
import com.zipdamember.domain.agent.entity.AgentApplicationDocument;
import com.zipdamember.domain.agent.ocr.AgentDocumentOcrParser;
import com.zipdamember.domain.agent.ocr.DocumentOcrResult;
import com.zipdamember.domain.agent.ocr.DocumentTextExtractor;
import com.zipdamember.domain.agent.repository.AgentApplicationDocumentRepository;
import com.zipdamember.domain.agent.repository.AgentApplicationRepository;
import com.zipdamember.domain.agent.repository.AgentProfileRepository;
import com.zipdamember.domain.agent.request.AgentApplicationUpdateRequest;
import com.zipdamember.domain.agent.response.AgentApplicationDocumentResponse;
import com.zipdamember.domain.agent.response.AgentApplicationResponse;
import com.zipdamember.domain.agent.response.AgentDocumentDownloadResponse;
import com.zipdamember.domain.file.constant.FileCategory;
import com.zipdamember.domain.file.entity.FileObject;
import com.zipdamember.domain.file.service.FileService;
import com.zipdamember.domain.file.service.ValidatedDocument;
import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.error.custom.business.OcrProcessingException;
import com.zipdamember.global.response.constant.CustomResponseCode;
import com.zipdamember.global.security.constant.MemberRolePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentApplicationService {

    private static final int PRIVATE_URL_EXPIRY_SECONDS = 300;
    private static final List<AgentApplicationStatus> DUPLICATE_TARGET_STATUSES = List.of(
            AgentApplicationStatus.PENDING,
            AgentApplicationStatus.UNDER_REVIEW,
            AgentApplicationStatus.APPROVED
    );

    private final AgentApplicationRepository agentApplicationRepository;
    private final AgentApplicationDocumentRepository agentApplicationDocumentRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final FileService fileService;
    private final DocumentTextExtractor documentTextExtractor;
    private final AgentDocumentOcrParser agentDocumentOcrParser;

    @Transactional
    public AgentApplicationResponse createDraft(Long memberId) {
        validateApplicant(memberId);

        return agentApplicationRepository
                .findTopByMemberIdOrderByCreatedAtDescApplicationIdDesc(memberId)
                .map(application -> {
                    if (!application.isEditable()) {
                        throw new BusinessException(
                                CustomResponseCode.AGENT_APPLICATION_DUPLICATED,
                                "이미 제출했거나 완료된 중개사 전환 신청이 있습니다."
                        );
                    }
                    return toResponse(application);
                })
                .orElseGet(() -> toResponse(
                        agentApplicationRepository.saveAndFlush(AgentApplication.create(memberId))
                ));
    }

    @Transactional(readOnly = true)
    public AgentApplicationResponse getCurrent(Long memberId) {
        AgentApplication application = agentApplicationRepository
                .findTopByMemberIdOrderByCreatedAtDescApplicationIdDesc(memberId)
                .orElseThrow(this::applicationNotFound);
        return toResponse(application);
    }

    @Transactional
    public AgentApplicationResponse update(
            Long applicationId,
            AgentApplicationUpdateRequest request,
            Long memberId
    ) {
        AgentApplication application = getOwnedApplicationForUpdate(applicationId, memberId);
        ensureEditable(application);

        String businessNo = normalizeBusinessNumber(request.businessRegistrationNo());
        String agentRegistrationNo = normalizeAgentRegistrationNumber(
                request.agentRegistrationNo()
        );
        application.updateRequestedInformation(
                businessNo,
                request.startDate(),
                request.representativeName().trim(),
                agentRegistrationNo,
                request.agencyName().trim()
        );
        return toResponse(application);
    }

    @Transactional
    public AgentApplicationDocumentResponse uploadDocument(
            Long applicationId,
            AgentApplicationDocumentType documentType,
            MultipartFile multipartFile,
            Long memberId
    ) {
        AgentApplication application = getOwnedApplicationForUpdate(applicationId, memberId);
        ensureEditable(application);

        ValidatedDocument file = fileService.prepareAgentDocument(multipartFile);
        DocumentOcrResult ocrResult = null;
        String ocrFailureReason = null;
        try {
            String rawText = documentTextExtractor.extract(file.content(), file.contentType());
            ocrResult = agentDocumentOcrParser.parse(documentType, rawText);
        } catch (OcrProcessingException exception) {
            ocrFailureReason = exception.getMessage();
        }

        FileObject storedFile = fileService.storePrivateAgentDocument(
                file,
                memberId,
                resolveFileCategory(documentType)
        );

        agentApplicationDocumentRepository
                .findTopByApplicationIdAndDocumentTypeOrderByUploadedAtDescDocumentIdDesc(
                        applicationId,
                        documentType
                )
                .ifPresent(agentApplicationDocumentRepository::delete);

        AgentApplicationDocument document = AgentApplicationDocument.create(
                applicationId,
                storedFile.getFileId(),
                documentType,
                storedFile.getChecksum()
        );
        LocalDateTime now = LocalDateTime.now();
        if (ocrResult == null) {
            document.failOcr(ocrFailureReason, now);
        } else {
            document.completeOcr(ocrResult, now);
            application.applyOcrSuggestion(ocrResult);
        }
        AgentApplicationDocument savedDocument =
                agentApplicationDocumentRepository.saveAndFlush(document);

        return toDocumentResponse(savedDocument);
    }

    @Transactional
    public AgentApplicationResponse submit(Long applicationId, Long memberId) {
        AgentApplication application = getOwnedApplicationForUpdate(applicationId, memberId);
        ensureEditable(application);
        MemberAccount member = validateApplicant(memberId);

        validateRequiredInformation(application, member);
        validateRequiredDocuments(applicationId);
        validateDuplicates(application);

        application.submit(LocalDateTime.now());
        return toResponse(application);
    }

    @Transactional(readOnly = true)
    public AgentDocumentDownloadResponse getDocumentDownloadUrl(
            Long applicationId,
            Long documentId,
            Long memberId
    ) {
        getOwnedApplication(applicationId, memberId);
        AgentApplicationDocument document = agentApplicationDocumentRepository
                .findByDocumentIdAndApplicationId(documentId, applicationId)
                .orElseThrow(() -> new BusinessException(
                        CustomResponseCode.NOT_FOUND_RESOURCE_ERROR,
                        "중개사 신청 서류를 찾을 수 없습니다."
                ));
        return new AgentDocumentDownloadResponse(
                fileService.createPrivateDownloadUrl(document.getFileId()),
                PRIVATE_URL_EXPIRY_SECONDS
        );
    }

    private MemberAccount validateApplicant(Long memberId) {
        MemberAccount member = memberAccountRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        CustomResponseCode.NOT_FOUND_RESOURCE_ERROR,
                        "회원 정보를 찾을 수 없습니다."
                ));
        if (member.getStatus() != MemberStatus.ACTIVE
                || member.getMemberRole() != MemberRolePolicy.USER
                || agentProfileRepository.existsByMemberId(memberId)) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "활성 일반 회원만 중개사 전환을 신청할 수 있습니다."
            );
        }
        return member;
    }

    private void validateRequiredInformation(
            AgentApplication application,
            MemberAccount member
    ) {
        if (isBlank(application.getRequestBusinessNo())
                || application.getRequestStartDate() == null
                || isBlank(application.getRequestRepresentativeName())
                || isBlank(application.getRequestAgencyRegistrationNo())
                || isBlank(application.getRequestAgencyName())) {
            throw invalidRequest("필수 신청 정보를 모두 입력해야 합니다.");
        }
        if (!normalizeName(member.getName()).equals(
                normalizeName(application.getRequestRepresentativeName()))) {
            throw invalidRequest("대표자명은 회원가입 시 입력한 이름과 일치해야 합니다.");
        }
    }

    private void validateRequiredDocuments(Long applicationId) {
        for (AgentApplicationDocumentType documentType
                : AgentApplicationDocumentType.values()) {
            AgentApplicationDocument document = agentApplicationDocumentRepository
                    .findTopByApplicationIdAndDocumentTypeOrderByUploadedAtDescDocumentIdDesc(
                            applicationId,
                            documentType
                    )
                    .orElseThrow(() -> invalidRequest(
                            documentType.name() + " 서류를 업로드해야 합니다."
                    ));
            if (!document.isOcrCompleted()) {
                throw invalidRequest(documentType.name() + " 서류의 OCR 처리가 완료되지 않았습니다.");
            }
        }
    }

    private void validateDuplicates(AgentApplication application) {
        if (agentProfileRepository.existsByBusinessRegistrationNo(
                application.getRequestBusinessNo())) {
            throw duplicateApplication("이미 등록된 사업자등록번호입니다.");
        }
        if (agentApplicationRepository
                .existsByRequestBusinessNoAndApplicationIdNotAndSubmittedAtIsNotNullAndStatusIn(
                        application.getRequestBusinessNo(),
                        application.getApplicationId(),
                        DUPLICATE_TARGET_STATUSES
                )) {
            throw duplicateApplication("동일한 사업자등록번호로 진행 중인 신청이 있습니다.");
        }
        if (agentApplicationRepository
                .existsByRequestAgencyRegistrationNoAndApplicationIdNotAndSubmittedAtIsNotNullAndStatusIn(
                        application.getRequestAgencyRegistrationNo(),
                        application.getApplicationId(),
                        DUPLICATE_TARGET_STATUSES
                )) {
            throw duplicateApplication("동일한 중개사무소 개설등록번호로 진행 중인 신청이 있습니다.");
        }
    }

    private AgentApplication getOwnedApplicationForUpdate(Long applicationId, Long memberId) {
        return agentApplicationRepository.findByApplicationIdAndMemberId(applicationId, memberId)
                .orElseThrow(this::applicationNotFound);
    }

    private AgentApplication getOwnedApplication(Long applicationId, Long memberId) {
        return agentApplicationRepository.findById(applicationId)
                .filter(application -> application.getMemberId().equals(memberId))
                .orElseThrow(this::applicationNotFound);
    }

    private void ensureEditable(AgentApplication application) {
        if (!application.isEditable()) {
            throw new BusinessException(
                    CustomResponseCode.AGENT_APPLICATION_NOT_EDITABLE,
                    "제출 완료 또는 심사 중인 신청은 수정할 수 없습니다."
            );
        }
    }

    private AgentApplicationResponse toResponse(AgentApplication application) {
        List<AgentApplicationDocumentResponse> documents =
                agentApplicationDocumentRepository
                        .findAllByApplicationIdOrderByUploadedAtDescDocumentIdDesc(
                                application.getApplicationId()
                        )
                        .stream()
                        .map(this::toDocumentResponse)
                        .toList();
        return AgentApplicationResponse.of(application, documents);
    }

    private AgentApplicationDocumentResponse toDocumentResponse(
            AgentApplicationDocument document
    ) {
        return AgentApplicationDocumentResponse.of(
                document,
                fileService.createPrivateDownloadUrl(document.getFileId())
        );
    }

    private FileCategory resolveFileCategory(AgentApplicationDocumentType documentType) {
        return documentType == AgentApplicationDocumentType.BUSINESS_LICENSE
                ? FileCategory.BUSINESS_LICENSE
                : FileCategory.AGENT_CERTIFICATE;
    }

    private String normalizeBusinessNumber(String value) {
        String normalized = value.replaceAll("\\D", "");
        if (normalized.length() != 10) {
            throw invalidRequest("사업자등록번호는 숫자 10자리여야 합니다.");
        }
        return normalized;
    }

    private String normalizeAgentRegistrationNumber(String value) {
        String normalized = value.trim()
                .replaceAll("[‐‑‒–—]", "-")
                .replaceAll("\\s+", "")
                .replaceFirst("^제", "")
                .replaceFirst("호$", "");
        if (normalized.length() < 3 || normalized.length() > 20
                || !normalized.matches("^[가-힣A-Za-z0-9-]+$")
                || !normalized.matches(".*\\d.*")) {
            throw invalidRequest("중개사무소 개설등록번호 형식이 올바르지 않습니다.");
        }
        return normalized;
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BusinessException invalidRequest(String message) {
        return new BusinessException(CustomResponseCode.INVALID_PARAMETER_ERROR, message);
    }

    private BusinessException duplicateApplication(String message) {
        return new BusinessException(CustomResponseCode.AGENT_APPLICATION_DUPLICATED, message);
    }

    private BusinessException applicationNotFound() {
        return new BusinessException(
                CustomResponseCode.AGENT_APPLICATION_NOT_FOUND,
                "중개사 전환 신청을 찾을 수 없습니다."
        );
    }
}
