package com.zipdamember.domain.agent.controller;

import com.zipdamember.domain.agent.constant.AgentApplicationDocumentType;
import com.zipdamember.domain.agent.request.AgentApplicationUpdateRequest;
import com.zipdamember.domain.agent.response.AgentApplicationDocumentResponse;
import com.zipdamember.domain.agent.response.AgentApplicationResponse;
import com.zipdamember.domain.agent.response.AgentDocumentDownloadResponse;
import com.zipdamember.domain.agent.service.AgentApplicationService;
import com.zipdamember.global.config.openapi.CustomApiResponse;
import com.zipdamember.global.response.GlobalResponseDTO;
import com.zipdamember.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@Tag(name = "회원 중개사 전환 신청 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/agent-applications")
public class AgentApplicationController {

    private final AgentApplicationService agentApplicationService;

    @Operation(summary = "중개사 전환 신청 초안 생성")
    @CustomApiResponse(value = {
            CustomResponseCode.AGENT_APPLICATION_DUPLICATED,
            CustomResponseCode.INVALID_PARAMETER_ERROR,
            CustomResponseCode.UNAUTHENTICATED_ERROR,
            CustomResponseCode.UNAUTHORIZED_ERROR
    })
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<GlobalResponseDTO<AgentApplicationResponse>> createDraft(Authentication authentication) {
        AgentApplicationResponse response = agentApplicationService.createDraft(authentication.getName());
        // URI.create("/api/member/agent-applications/": URI.create(...)는 응답의 Location 헤더를 만듦
        // Location 헤더: 금 생성한 리소스의 주소는 여기입니다”라고 알려주는 HTTP 표준 응답 헤더
        return ResponseEntity.created(URI.create("/api/member/agent-applications/" + response.applicationId())).body(GlobalResponseDTO.success(response));
    }

    @Operation(summary = "내 최근 중개사 전환 신청 조회")
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @GetMapping("/current")
    public ResponseEntity<GlobalResponseDTO<AgentApplicationResponse>> getCurrent(
            Authentication authentication
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                agentApplicationService.getCurrent(authentication.getName())
        ));
    }

    @Operation(summary = "OCR 추출값 확인 후 신청 정보 수정")
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/{applicationId}")
    public ResponseEntity<GlobalResponseDTO<AgentApplicationResponse>> update(
            @PathVariable Long applicationId,
            @Valid @RequestBody AgentApplicationUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                agentApplicationService.update(
                        applicationId,
                        request,
                        authentication.getName()
                )
        ));
    }

    @Operation(summary = "사업자등록증 또는 중개사무소등록증 업로드 및 OCR")
    @CustomApiResponse(value = {
            CustomResponseCode.FILE_MANAGED_ERROR,
            CustomResponseCode.OCR_PROCESSING_ERROR,
            CustomResponseCode.AGENT_APPLICATION_NOT_EDITABLE,
            CustomResponseCode.AGENT_APPLICATION_NOT_FOUND
    })
    @PreAuthorize("hasRole('USER')")
    @PostMapping(
            value = "/{applicationId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<GlobalResponseDTO<AgentApplicationDocumentResponse>> uploadDocument(
            @PathVariable Long applicationId,
            @RequestParam AgentApplicationDocumentType documentType,
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                agentApplicationService.uploadDocument(
                        applicationId,
                        documentType,
                        file,
                        authentication.getName()
                )
        ));
    }

    @Operation(summary = "중개사 전환 신청 제출 또는 재제출")
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{applicationId}/submissions")
    public ResponseEntity<GlobalResponseDTO<AgentApplicationResponse>> submit(
            @PathVariable Long applicationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                agentApplicationService.submit(applicationId, authentication.getName())
        ));
    }

    @Operation(summary = "내 신청 서류의 5분 만료 Signed URL 발급")
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @GetMapping("/{applicationId}/documents/{documentId}/download-url")
    public ResponseEntity<GlobalResponseDTO<AgentDocumentDownloadResponse>> getDownloadUrl(
            @PathVariable Long applicationId,
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                agentApplicationService.getDocumentDownloadUrl(applicationId, documentId, authentication.getName())
        ));
    }
}
