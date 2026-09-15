package com.zipdamember.domain.verification.scheduler;

import com.zipdamember.domain.verification.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationCleanupScheduler {

    private final VerificationService verificationService;

    @Scheduled(
            cron = "${zipda.verification.cleanup.cron}",
            zone = "${zipda.verification.cleanup.zone}"
    )
    public void deleteExpiredVerifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        long deletedCount = verificationService.deleteExpiredVerifications(cutoff);

        log.info(
                "만료 이메일 인증 데이터 물리 삭제 완료: cutoff={}, deletedCount={}",
                cutoff,
                deletedCount
        );
    }
}
