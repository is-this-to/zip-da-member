package com.zipdamember.domain.member.controller;

import com.zipdamember.domain.member.constant.MemberPermissionAction;
import com.zipdamember.domain.member.response.MemberPermissionResponse;
import com.zipdamember.domain.member.service.MemberPermissionService;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.GlobalResponseDTO;
import com.zipdamember.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/members")
public class InternalMemberPermissionController {

    private final MemberPermissionService memberPermissionService;

    @GetMapping("/{memberId}/permissions")
    public ResponseEntity<GlobalResponseDTO<MemberPermissionResponse>> getPermission(
            @PathVariable String memberId,
            @RequestParam MemberPermissionAction action
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                memberPermissionService.getPermission(parseMemberId(memberId), action)
        ));
    }

    private Long parseMemberId(String memberId) {
        if (memberId == null || !memberId.matches("^[1-9]\\d*$")) {
            throw invalidMemberId();
        }

        try {
            return Long.parseLong(memberId);
        } catch (NumberFormatException exception) {
            throw invalidMemberId();
        }
    }

    private BusinessException invalidMemberId() {
        return new BusinessException(
                CustomResponseCode.INVALID_PARAMETER_ERROR,
                "올바르지 않은 회원 식별자입니다."
        );
    }
}
