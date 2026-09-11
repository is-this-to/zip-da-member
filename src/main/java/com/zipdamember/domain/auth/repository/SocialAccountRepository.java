package com.zipdamember.domain.auth.repository;

import com.zipdamember.domain.auth.entity.SocialAccount;
import com.zipdamember.global.security.constant.ProviderPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(
            ProviderPolicy provider,
            String providerUserId
    );

    boolean existsByProviderAndProviderUserId(
            ProviderPolicy provider,
            String providerUserId
    );
}
