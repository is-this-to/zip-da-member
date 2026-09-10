package com.zipdamember.domain.agent.constant;

public enum VerificationProvider {
    NTS("국세청"),
    MOLIT("국토교통부");

    private final String description;

    VerificationProvider(String description) {
        this.description = description;
    }
}
