package com.zipdamember.domain.verification.service;

import com.zipdamember.domain.verification.request.EmailDuplicateCheckRequest;
import com.zipdamember.domain.verification.response.RegistrationDuplicateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VerificationService {

    public RegistrationDuplicateResponse checkDuplicateEmail(EmailDuplicateCheckRequest emailDuplicateCheckRequest) {

    }
}
