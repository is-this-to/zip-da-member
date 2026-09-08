package com.zipdamember.global.config.verification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zipda.verification.cleanup")
public record EmailVerificationConfig(
        String cron,
        String zone
) {
}
