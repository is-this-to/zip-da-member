package com.zipdamember.domain.file.controller;

import com.zipdamember.domain.file.response.FileUploadResponse;
import com.zipdamember.domain.file.service.FileService;
import com.zipdamember.global.config.openapi.CustomApiResponse;
import com.zipdamember.global.response.GlobalResponseDTO;
import com.zipdamember.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "회원 파일 API", description = "회원가입 파일 업로드")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/files")
public class FileController {

    private final FileService fileService;

    /**
     * 회원가입 프로필 사진 업로드
     * @param file 회원가입 프로필 사진 파일
     * @return 저장한 파일 식별자, 파일 주소
     */
    @Operation(summary = "회원가입 프로필 사진 업로드")
    @SecurityRequirements
    @CustomApiResponse(value = {
            CustomResponseCode.FILE_MANAGED_ERROR,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("!isAuthenticated()")
    @PostMapping(value = "/profiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponseDTO<FileUploadResponse>> uploadProfile(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(GlobalResponseDTO.success(fileService.uploadProfile(file)));
    }
}
