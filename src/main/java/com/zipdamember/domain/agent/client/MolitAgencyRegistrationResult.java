package com.zipdamember.domain.agent.client;

import com.zipdamember.domain.agent.constant.VerificationResultStatus;

public record MolitAgencyRegistrationResult(
        VerificationResultStatus resultStatus,
        String businessStatus,
        String roadAddress,
        String jibunAddress
) {
    public static MolitAgencyRegistrationResult matched(
            String businessStatus,
            String roadAddress,
            String jibunAddress
    ) {
        return new MolitAgencyRegistrationResult(
                VerificationResultStatus.MATCHED,
                businessStatus,
                roadAddress,
                jibunAddress
        );
    }

    public static MolitAgencyRegistrationResult mismatched() {
        return new MolitAgencyRegistrationResult(
                VerificationResultStatus.MISMATCHED,
                null,
                null,
                null
        );
    }

    public static MolitAgencyRegistrationResult error() {
        return new MolitAgencyRegistrationResult(
                VerificationResultStatus.ERROR,
                null,
                null,
                null
        );
    }
}
