package com.zipdamember.global.config.ocr;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zipda.ocr")
public record OcrConfig(
        boolean enabled,
        String projectId,
        String inputBucket,
        String outputBucket,
        long timeoutSeconds,
        long maxFileSizeBytes,
        int maxPdfPages
) {
}
