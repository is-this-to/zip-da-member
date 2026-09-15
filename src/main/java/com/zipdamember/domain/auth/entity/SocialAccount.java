package com.zipdamember.domain.auth.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.zipdamember.global.security.constant.ProviderPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "social_account",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_social_account_provider_user",
                columnNames = {"provider", "provider_user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    @Id
    @Column(name = "social_account_id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long socialAccountId;

    @Column(name = "member_id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, updatable = false, length = 20)
    private ProviderPolicy provider;

    @Column(name = "provider_user_id", nullable = false, updatable = false, length = 100)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static SocialAccount create(
            Long memberId,
            ProviderPolicy provider,
            String providerUserId,
            String providerEmail
    ) {
        SocialAccount account = new SocialAccount();
        account.memberId = memberId;
        account.provider = provider;
        account.providerUserId = providerUserId;
        account.providerEmail = providerEmail;
        return account;
    }

    @PrePersist
    private void generateId() {
        if (socialAccountId == null) {
            socialAccountId = TsidCreator.getTsid().toLong();
        }
    }
}
