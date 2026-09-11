package com.zipdamember.domain.agent.service;

import com.zipdamember.domain.agent.constant.AgentProfileImageUpdatePolicy;
import com.zipdamember.domain.agent.entity.AgentBusinessHour;
import com.zipdamember.domain.agent.entity.AgentProfile;
import com.zipdamember.domain.agent.entity.AgentSpecialty;
import com.zipdamember.domain.agent.event.AgentProfileUpdatedEvent;
import com.zipdamember.domain.agent.repository.AgentBusinessHourRepository;
import com.zipdamember.domain.agent.repository.AgentProfileRepository;
import com.zipdamember.domain.agent.repository.AgentSpecialtyRepository;
import com.zipdamember.domain.agent.request.AgentBusinessHourRequest;
import com.zipdamember.domain.agent.request.AgentPublicProfileUpdateRequest;
import com.zipdamember.domain.agent.request.AgentSpecialtyRequest;
import com.zipdamember.domain.agent.response.AgentBusinessHourResponse;
import com.zipdamember.domain.agent.response.AgentPublicProfileResponse;
import com.zipdamember.domain.agent.response.AgentSpecialtyResponse;
import com.zipdamember.domain.file.constant.FileCategory;
import com.zipdamember.domain.file.service.FileService;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AgentPublicProfileService {
    private final AgentProfileRepository agentProfileRepository;
    private final AgentSpecialtyRepository specialtyRepository;
    private final AgentBusinessHourRepository businessHourRepository;
    private final FileService fileService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public AgentPublicProfileResponse getPublicProfile(Long agentId) {
        return toResponse(getAgent(agentId));
    }

    @Transactional
    public AgentPublicProfileResponse updateMyProfile(
            Long agentId,
            Long memberId,
            AgentPublicProfileUpdateRequest request
    ) {
        AgentProfile profile = getAgent(agentId);
        if (!profile.getMemberId().equals(memberId)) {
            throw new BusinessException(
                    CustomResponseCode.UNAUTHORIZED_ERROR,
                    "본인의 중개사 프로필만 수정할 수 있습니다."
            );
        }
        if (request.intro() == null
                && request.profileImageAction() == null
                && request.specialties() == null
                && request.businessHours() == null) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "변경할 중개사 프로필 정보를 한 개 이상 전달해야 합니다."
            );
        }

        AgentProfileImageUpdatePolicy imageAction = request.profileImageAction() == null
                ? AgentProfileImageUpdatePolicy.KEEP
                : request.profileImageAction();
        Long profileFileId = resolveProfileFileId(profile, memberId, request, imageAction);
        profile.updatePublicProfile(request.intro(), profileFileId);

        if (request.specialties() != null) replaceSpecialties(agentId, request.specialties());
        if (request.businessHours() != null) replaceBusinessHours(agentId, request.businessHours());
        eventPublisher.publishEvent(new AgentProfileUpdatedEvent(agentId.toString(), memberId.toString()));
        return toResponse(profile);
    }

    private void replaceSpecialties(Long agentId, List<AgentSpecialtyRequest> requests) {
        Set<String> regionCodes = new HashSet<>();
        List<AgentSpecialty> specialties = requests.stream()
                .map(request -> {
                    String regionCode = request.regionCode().trim();
                    if (!regionCodes.add(regionCode)) {
                        throw new BusinessException(
                                CustomResponseCode.INVALID_PARAMETER_ERROR,
                                "전문 지역 코드는 중복될 수 없습니다."
                        );
                    }
                    return AgentSpecialty.create(
                            agentId,
                            regionCode,
                            request.regionName().trim(),
                            request.displayOrder()
                    );
                })
                .toList();
        specialtyRepository.deleteAllByAgentId(agentId);
        specialtyRepository.flush();
        specialtyRepository.saveAll(specialties);
    }

    private void replaceBusinessHours(Long agentId, List<AgentBusinessHourRequest> requests) {
        Set<Object> days = new HashSet<>();
        List<AgentBusinessHour> hours = requests.stream()
                .map(request -> {
                    if (!days.add(request.dayOfWeek())) {
                        throw new BusinessException(
                                CustomResponseCode.INVALID_PARAMETER_ERROR,
                                "같은 요일의 영업시간을 중복 등록할 수 없습니다."
                        );
                    }
                    try {
                        return AgentBusinessHour.create(
                                agentId,
                                request.dayOfWeek(),
                                request.openTime(),
                                request.closeTime(),
                                request.closed()
                        );
                    } catch (IllegalArgumentException exception) {
                        throw new BusinessException(
                                CustomResponseCode.INVALID_PARAMETER_ERROR,
                                exception.getMessage()
                        );
                    }
                })
                .toList();
        businessHourRepository.deleteAllByAgentId(agentId);
        businessHourRepository.flush();
        businessHourRepository.saveAll(hours);
    }

    private Long resolveProfileFileId(
            AgentProfile profile,
            Long memberId,
            AgentPublicProfileUpdateRequest request,
            AgentProfileImageUpdatePolicy action
    ) {
        if (action == AgentProfileImageUpdatePolicy.KEEP) return profile.getProfileFileId();
        if (action == AgentProfileImageUpdatePolicy.REMOVE) return null;
        if (request.profileFileId() == null || request.profileFileId().isBlank()) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "교체할 중개사 대표 이미지 식별자는 필수입니다."
            );
        }
        try {
            Long fileId = Long.parseLong(request.profileFileId());
            fileService.validateOwnedProfile(fileId, memberId, FileCategory.AGENT_PROFILE);
            return fileId;
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "올바르지 않은 중개사 대표 이미지 식별자입니다."
            );
        }
    }

    private AgentPublicProfileResponse toResponse(AgentProfile profile) {
        List<AgentSpecialtyResponse> specialties = specialtyRepository
                .findAllByAgentIdOrderByDisplayOrderAsc(profile.getAgentId())
                .stream().map(AgentSpecialtyResponse::from).toList();
        List<AgentBusinessHourResponse> hours = businessHourRepository
                .findAllByAgentIdOrderByDayOfWeekAsc(profile.getAgentId())
                .stream().map(AgentBusinessHourResponse::from).toList();
        return AgentPublicProfileResponse.from(
                profile,
                fileService.getPublicFileUri(profile.getProfileFileId()),
                specialties,
                hours
        );
    }

    private AgentProfile getAgent(Long agentId) {
        return agentProfileRepository.findByAgentId(agentId)
                .orElseThrow(() -> new BusinessException(
                        CustomResponseCode.NOT_FOUND_RESOURCE_ERROR,
                        "중개사 프로필을 찾을 수 없습니다."
                ));
    }
}
