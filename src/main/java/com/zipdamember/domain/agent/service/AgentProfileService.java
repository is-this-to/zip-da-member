package com.zipdamember.domain.agent.service;

import com.querydsl.core.BooleanBuilder;
import com.zipdamember.domain.agent.entity.AgentProfile;
import com.zipdamember.domain.agent.entity.QAgentProfile;
import com.zipdamember.domain.agent.repository.AgentProfileRepository;
import com.zipdamember.domain.agent.request.AdminAgentProfileSearchRequest;
import com.zipdamember.domain.agent.response.AdminAgentProfileListResponse;
import com.zipdamember.domain.agent.response.AdminAgentProfileResponse;
import com.zipdamember.global.error.custom.business.DuplicatedResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class AgentProfileService {
    private final AgentProfileRepository agentProfileRepository;

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

    /** LIKE 와일드카드 이스케이프 및 부분 일치 검색 패턴 생성 */
    private String likePattern(String value) {
        return value == null ? null
                : "%" + value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_") + "%";
    }
}
