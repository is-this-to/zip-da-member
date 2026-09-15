package com.zipdamember.global.config.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zipda.password-reset")
public record PasswordResetConfig(String frontendResetUri, int tokenExpiryMinutes) {
}
