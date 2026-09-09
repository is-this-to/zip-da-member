package com.zipdamember.domain.agent.service;

import com.zipdamember.domain.agent.entity.AgentApplication;
import com.zipdamember.domain.agent.repository.AgencyRegistrationVerificationRepository;
import com.zipdamember.domain.agent.repository.AgentApplicationDocumentRepository;
import com.zipdamember.domain.agent.repository.AgentApplicationRepository;
import com.zipdamember.domain.agent.repository.AdminAgentApplicationQueryRepository;
import com.zipdamember.domain.agent.repository.BusinessVerificationRepository;
import com.zipdamember.domain.agent.request.AdminAgentApplicationSearchRequest;
import com.zipdamember.domain.agent.response.AdminAgencyRegistrationVerificationResponse;
import com.zipdamember.domain.agent.response.AdminAgentApplicationDetailResponse;
import com.zipdamember.domain.agent.response.AdminAgentApplicationDocumentResponse;
import com.zipdamember.domain.agent.response.AdminAgentApplicationListResponse;
import com.zipdamember.domain.agent.response.AdminBusinessVerificationResponse;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.global.error.custom.business.NotFoundResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAgentApplicationService {

    private final AdminAgentApplicationQueryRepository adminAgentApplicationQueryRepository;
    private final AgentApplicationRepository agentApplicationRepository;
    private final AgentApplicationDocumentRepository agentApplicationDocumentRepository;
    private final BusinessVerificationRepository businessVerificationRepository;
    private final AgencyRegistrationVerificationRepository agencyRegistrationVerificationRepository;
    private final MemberAccountRepository memberAccountRepository;

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
                .map(AdminAgentApplicationDocumentResponse::from)
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
}
