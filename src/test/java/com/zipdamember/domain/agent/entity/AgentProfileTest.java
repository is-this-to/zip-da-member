package com.zipdamember.domain.agent.entity;

import com.zipdamember.domain.agent.constant.AgentOperatingStatus;
import com.zipdamember.domain.agent.repository.AgentProfileRepository;
import com.zipdamember.domain.agent.request.AdminAgentProfileSearchRequest;
import com.zipdamember.domain.agent.service.AgentProfileService;
import com.zipdamember.global.error.custom.business.DuplicatedResourceException;
import com.zipdamember.global.jpa.JPAWithDeleted;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentProfileTest {

    @Test
    void newProfile_hasActiveOperatingStatus() {
        var profile = new AgentProfile(
                1L,
                "120-12-34567",
                "ZIPDA 공인중개사무소",
                "홍길동",
                "02-1234-5678",
                "서울특별시 강남구",
                LocalDateTime.now()
        );

        assertThat(profile.getOperatingStatus()).isEqualTo(AgentOperatingStatus.ACTIVE);
    }

    @Test
    void businessRegistrationNumberCheck_includesDeletedProfiles() throws NoSuchMethodException {
        var method = AgentProfileRepository.class.getMethod(
                "existsByBusinessRegistrationNo", String.class);

        assertThat(method.getAnnotation(JPAWithDeleted.class)).isNotNull();
    }

    @Test
    void register_duplicateBusinessRegistrationNumber_isRejected() {
        var repository = mock(AgentProfileRepository.class);
        var profile = profile("120-12-34567");
        when(repository.existsByBusinessRegistrationNo("120-12-34567")).thenReturn(true);

        assertThatThrownBy(() -> new AgentProfileService(repository).register(profile))
                .isInstanceOf(DuplicatedResourceException.class)
                .hasMessage("이미 등록된 사업자등록번호입니다.");
        verify(repository, never()).save(profile);
    }

    @Test
    void register_availableBusinessRegistrationNumber_savesProfile() {
        var repository = mock(AgentProfileRepository.class);
        var profile = profile("120-12-34567");
        when(repository.save(profile)).thenReturn(profile);

        var saved = new AgentProfileService(repository).register(profile);

        assertThat(saved).isSameAs(profile);
        verify(repository).save(profile);
    }

    private AgentProfile profile(String businessRegistrationNo) {
        return new AgentProfile(
                1L,
                businessRegistrationNo,
                "ZIPDA 공인중개사무소",
                "홍길동",
                "02-1234-5678",
                "서울특별시 강남구",
                LocalDateTime.now()
        );
    }
}
