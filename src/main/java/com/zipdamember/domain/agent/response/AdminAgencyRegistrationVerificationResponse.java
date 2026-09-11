package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.VerificationProvider;
import com.zipdamember.domain.agent.constant.VerificationResultStatus;
import com.zipdamember.domain.agent.constant.VerificationType;
import com.zipdamember.domain.agent.entity.AgencyRegistrationVerification;

import java.time.LocalDateTime;

public record AdminAgencyRegistrationVerificationResponse(
        VerificationProvider provider,
        VerificationType verificationType,
        VerificationResultStatus resultStatus,
        String businessStatus,
        String jibunAddress,
        String roadAddress,
        LocalDateTime checkedAt
) {
    public static AdminAgencyRegistrationVerificationResponse from(
            AgencyRegistrationVerification verification
    ) {
        return new AdminAgencyRegistrationVerificationResponse(
                verification.getProvider(),
                verification.getVerificationType(),
                verification.getResultStatus(),
                verification.getBusinessStatus(),
                verification.getJibunAddress(),
                verification.getRoadAddress(),
                verification.getCheckedAt()
        );
    }
}
