package com.zipdamember.domain.agent.client;

import com.zipdamember.domain.agent.constant.VerificationResultStatus;

public record NtsBusinessValidationResult(
        VerificationResultStatus resultStatus
) {
    public static NtsBusinessValidationResult matched() {
        return new NtsBusinessValidationResult(VerificationResultStatus.MATCHED);
    }

    public static NtsBusinessValidationResult mismatched() {
        return new NtsBusinessValidationResult(VerificationResultStatus.MISMATCHED);
    }

    public static NtsBusinessValidationResult error() {
        return new NtsBusinessValidationResult(VerificationResultStatus.ERROR);
    }
}
