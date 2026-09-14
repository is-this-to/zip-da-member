package com.zipdamember.domain.agent.ocr;

import java.time.LocalDate;

public record DocumentOcrResult(
        String rawText,
        String businessRegistrationNo,
        LocalDate startDate,
        String representativeName,
        String agentRegistrationNo,
        String agencyName
) {
}
