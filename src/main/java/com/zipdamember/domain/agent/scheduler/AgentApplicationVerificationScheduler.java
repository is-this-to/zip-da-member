package com.zipdamember.domain.agent.scheduler;

import com.zipdamember.domain.agent.service.AdminAgentApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AgentApplicationVerificationScheduler {

    private final AdminAgentApplicationService adminAgentApplicationService;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void verifyPendingApplications() {
        adminAgentApplicationService.verifyPendingApplications();
    }
}
