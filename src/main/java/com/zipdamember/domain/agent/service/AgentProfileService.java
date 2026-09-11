package com.zipdamember.domain.agent.service;

import com.querydsl.core.BooleanBuilder;
import com.zipdamember.domain.agent.entity.AgentProfile;
import com.zipdamember.domain.agent.entity.QAgentProfile;
import com.zipdamember.domain.agent.repository.AgentProfileRepository;
import com.zipdamember.domain.agent.request.AdminAgentOperatingStatusChangeRequest;
import com.zipdamember.domain.agent.request.AdminAgentOperatingStatusSearchRequest;
import com.zipdamember.domain.agent.request.AdminAgentProfileSearchRequest;
import com.zipdamember.domain.agent.response.AdminAgentOperatingStatusListResponse;
import com.zipdamember.domain.agent.response.AdminAgentOperatingStatusResponse;
import com.zipdamember.domain.agent.response.AdminAgentProfileListResponse;
import com.zipdamember.domain.agent.response.AdminAgentProfileResponse;
import com.zipdamember.domain.admin.constant.AdminAuditAction;
import com.zipdamember.domain.admin.constant.AdminAuditActorType;
import com.zipdamember.domain.admin.constant.AdminAuditTargetService;
import com.zipdamember.domain.admin.constant.AdminAuditValueType;
import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.admin.request.AdminAuditLogWriteRequest;
import com.zipdamember.domain.admin.service.AdminAuditLogService;
import com.zipdamember.global.error.custom.business.DuplicatedResourceException;
import com.zipdamember.global.error.custom.business.NotFoundResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class AgentProfileService {
    private final AgentProfileRepository agentProfileRepository;
    private final AdminAuditLogService adminAuditLogService;

    @Transactional
    public AgentProfile register(AgentProfile agentProfile) {
        if (agentProfileRepository.existsByBusinessRegistrationNo(
                agentProfile.getBusinessRegistrationNo())) {
            throw new DuplicatedResourceException("이미 등록된 사업자등록번호입니다.");
        }
        return agentProfileRepository.save(agentProfile);
    }

    @Transactional(readOnly = true)
    public AdminAgentProfileListResponse search(AdminAgentProfileSearchRequest request) {
        QAgentProfile agentProfile = QAgentProfile.agentProfile;
        BooleanBuilder condition = new BooleanBuilder();

        if (request.agencyName() != null) {
            condition.and(agentProfile.agencyName
                    .like(likePattern(request.agencyName()), '!')); // 중개소명 부분일치
        }
        if (request.representativeName() != null) {
            condition.and(agentProfile.representativeName
                    .like(likePattern(request.representativeName()), '!')); // 대표자명 부분일치
        }
        if (request.businessRegistrationNo() != null) {
            condition.and(agentProfile.businessRegistrationNo
                    .eq(request.businessRegistrationNo())); // 사업자번호 일치
        }

        var pageable = PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(
                        Sort.Order.desc("approvedAt"), // 승인일 최신순
                        Sort.Order.desc("agentId")     // 동일 승인일 ID 내림차순
                )
        );

        return AdminAgentProfileListResponse.from(
                agentProfileRepository.findAll(condition, pageable) // 조건·페이징 조회
                        .map(AdminAgentProfileResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public AdminAgentOperatingStatusListResponse searchOperatingStatuses(
            AdminAgentOperatingStatusSearchRequest request
    ) {
        // 상태 관리 페이지 정렬 조건
        var pageable = PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(
                        Sort.Order.desc("statusChangedAt").nullsLast(),
                        Sort.Order.desc("agentId")
                )
        );

        // 중개소명 조건 목록 조회
        var profiles = request.agencyName() == null
                ? agentProfileRepository.findAll(pageable)
                : agentProfileRepository.findAllByAgencyNameContaining(
                        request.agencyName(), pageable
                );

        // 상태 관리 목록 응답
        return AdminAgentOperatingStatusListResponse.from(
                profiles.map(AdminAgentOperatingStatusResponse::from)
        );
    }

    @Transactional
    public AdminAgentOperatingStatusResponse changeOperatingStatus(
            Long agentId,
            AdminAgentOperatingStatusChangeRequest request,
            Long operatorAdminId,
            AdminRoleCode operatorRole,
            String ipAddress,
            String userAgent
    ) {
        // 중개소 프로필 조회
        AgentProfile profile = agentProfileRepository.findByAgentId(agentId)
                .orElseThrow(() -> new NotFoundResourceException("중개소 프로필을 찾을 수 없습니다."));

        // 기존 영업 상태 확인
        if (profile.getOperatingStatus() == request.operatingStatus()) {
            return AdminAgentOperatingStatusResponse.from(profile);
        }

        // 영업 상태 변경
        var beforeOperatingStatus = profile.getOperatingStatus();
        profile.changeOperatingStatus(request.operatingStatus(), operatorAdminId);

        // 영업 상태 변경 감사 로그
        adminAuditLogService.recordSuccess(new AdminAuditLogWriteRequest(
                operatorAdminId,
                AdminAuditActorType.ADMIN,
                operatorRole,
                AdminAuditAction.AGENCY_OPERATING_STATUS_CHANGE,
                AdminAuditTargetService.MEMBER,
                "AGENT_PROFILE",
                profile.getAgentId().toString(),
                request.reason(),
                ipAddress,
                userAgent,
                java.util.List.of(new AdminAuditLogWriteRequest.Change(
                        "operatingStatus",
                        beforeOperatingStatus.name(),
                        request.operatingStatus().name(),
                        AdminAuditValueType.ENUM,
                        0
                ))
        ));

        // 변경 상태 응답
        return AdminAgentOperatingStatusResponse.from(profile);
    }

    /** LIKE 와일드카드 이스케이프 및 부분 일치 검색 패턴 생성 */
    private String likePattern(String value) {
        return value == null ? null
                : "%" + value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_") + "%";
    }
}
