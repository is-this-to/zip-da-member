package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.VerificationProvider;
import com.zipdamember.domain.agent.constant.VerificationResultStatus;
import com.zipdamember.domain.agent.constant.VerificationType;
import com.zipdamember.domain.agent.entity.BusinessVerification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminBusinessVerificationResponse(
        VerificationProvider provider,
        VerificationType verificationType,
        VerificationResultStatus resultStatus,
        String businessStatus,
        String taxType,
        LocalDate closedAt,
        LocalDateTime checkedAt
) {
    public static AdminBusinessVerificationResponse from(BusinessVerification verification) {
        return new AdminBusinessVerificationResponse(
                verification.getProvider(),
                verification.getVerificationType(),
                verification.getResultStatus(),
                verification.getBusinessStatus(),
                verification.getTaxType(),
                verification.getClosedAt(),
                verification.getCheckedAt()
        );
    }
}
