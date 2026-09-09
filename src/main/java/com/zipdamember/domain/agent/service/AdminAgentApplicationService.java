package com.zipdamember.domain.agent.service;

import com.zipdamember.domain.agent.repository.AdminAgentApplicationQueryRepository;
import com.zipdamember.domain.agent.request.AdminAgentApplicationSearchRequest;
import com.zipdamember.domain.agent.response.AdminAgentApplicationListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAgentApplicationService {

    private final AdminAgentApplicationQueryRepository adminAgentApplicationQueryRepository;

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
}
