package com.zipdamember.domain.member.service;

import com.zipdamember.domain.agent.constant.AgentOperatingStatus;
import com.zipdamember.domain.agent.entity.AgentProfile;
import com.zipdamember.domain.agent.repository.AgentProfileRepository;
import com.zipdamember.domain.member.constant.MemberPermissionAction;
import com.zipdamember.domain.member.constant.MemberPermissionReason;
import com.zipdamember.domain.member.constant.MemberSanctionScope;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.domain.member.repository.MemberSanctionQueryRepository;
import com.zipdamember.domain.member.response.MemberPermissionResponse;
import com.zipdamember.global.security.constant.MemberRolePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberPermissionService {

    private final MemberAccountRepository memberAccountRepository;
    private final MemberSanctionQueryRepository memberSanctionQueryRepository;
    private final AgentProfileRepository agentProfileRepository;

    public MemberPermissionResponse getPermission(
            Long memberId,
            MemberPermissionAction action
    ) {
        if (memberId == null || memberId <= 0 || action == null) {
            return denied(MemberPermissionReason.MEMBER_NOT_FOUND);
        }

        MemberAccount member = memberAccountRepository.findById(memberId).orElse(null);
        if (member == null) {
            return denied(MemberPermissionReason.MEMBER_NOT_FOUND);
        }

        switch (member.getStatus()) {
            case WITHDRAWN -> {
                return denied(MemberPermissionReason.MEMBER_WITHDRAWN);
            }
            case SUSPENDED -> {
                return denied(MemberPermissionReason.MEMBER_SUSPENDED);
            }
            case LOCKED -> {
                return denied(MemberPermissionReason.MEMBER_LOCKED);
            }
            case ACTIVE -> {
                // 활성 회원은 다음 차단 조건을 검사합니다.
            }
        }

        MemberSanctionScope sanctionScope = memberSanctionQueryRepository
                .findActiveBlockingScope(memberId, LocalDateTime.now());
        if (sanctionScope == MemberSanctionScope.ACCOUNT) {
            return denied(MemberPermissionReason.ACCOUNT_SANCTIONED);
        }
        if (sanctionScope == MemberSanctionScope.PROPERTY) {
            return denied(MemberPermissionReason.PROPERTY_SANCTIONED);
        }

        if (member.getMemberRole() == MemberRolePolicy.AGENT) {
            AgentProfile agentProfile = agentProfileRepository.findByMemberId(memberId).orElse(null);
            if (agentProfile == null) {
                return denied(MemberPermissionReason.AGENT_PROFILE_NOT_FOUND);
            }
            if (agentProfile.getDeletedAt() != null
                    || agentProfile.getOperatingStatus() != AgentOperatingStatus.ACTIVE) {
                return denied(MemberPermissionReason.AGENT_NOT_ACTIVE);
            }
        }

        return MemberPermissionResponse.granted();
    }

    private MemberPermissionResponse denied(MemberPermissionReason reason) {
        return MemberPermissionResponse.denied(reason.name());
    }
}
