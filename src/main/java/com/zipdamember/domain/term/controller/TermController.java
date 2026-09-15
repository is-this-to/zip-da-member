package com.zipdamember.domain.term.controller;

import com.zipdamember.domain.term.response.TermResponse;
import com.zipdamember.domain.term.service.TermService;
import com.zipdamember.global.config.openapi.CustomApiResponse;
import com.zipdamember.global.response.GlobalResponseDTO;
import com.zipdamember.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

@Tag(name = "인증 약관 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class TermController {

    private final TermService termService;

    @Operation(summary = "약간 조회", description = "활성 상태에 맞는 약간을 조회한다")
    @CustomApiResponse(value = {
            CustomResponseCode.INVALID_PARAMETER_ERROR,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.DB_DUPLICATED_KEY_ERROR,
            CustomResponseCode.NOT_FOUND_RESOURCE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/terms")
    public ResponseEntity<GlobalResponseDTO<List<TermResponse>>> showTerms() {
        return ResponseEntity.ok(GlobalResponseDTO.success(termService.showTerms()));
    }
}
