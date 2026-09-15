package com.zipdamember.domain.agent.client;

import com.zipdamember.domain.agent.constant.VerificationResultStatus;

import java.time.LocalDate;

public record NtsBusinessValidationResult(
        VerificationResultStatus resultStatus,
        String businessStatus,
        String taxType,
        LocalDate closedAt
) {
    public static NtsBusinessValidationResult matched(
            String businessStatus,
            String taxType,
            LocalDate closedAt
    ) {
        return new NtsBusinessValidationResult(
                VerificationResultStatus.MATCHED,
                businessStatus,
                taxType,
                closedAt
        );
    }

    public static NtsBusinessValidationResult mismatched() {
        return new NtsBusinessValidationResult(
                VerificationResultStatus.MISMATCHED,
                null,
                null,
                null
        );
    }

    public static NtsBusinessValidationResult error() {
        return new NtsBusinessValidationResult(
                VerificationResultStatus.ERROR,
                null,
                null,
                null
        );
    }
}
