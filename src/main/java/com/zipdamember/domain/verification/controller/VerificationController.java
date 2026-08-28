package com.zipdamember.domain.verification.controller;

import com.zipdamember.domain.verification.request.EmailDuplicateCheckRequest;
import com.zipdamember.domain.verification.response.RegistrationDuplicateResponse;
import com.zipdamember.global.response.GlobalResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class VerificationController {

    @PostMapping("/email")
    public ResponseEntity<GlobalResponseDTO<RegistrationDuplicateResponse>> checkDuplicateEmail(@Valid EmailDuplicateCheckRequest emailDuplicateCheckRequest) {
        return ResponseEntity.ok()
    }
}
