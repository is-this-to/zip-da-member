package com.zipdamember.domain.term.service;

import com.zipdamember.domain.term.repository.TermRepository;
import com.zipdamember.domain.term.response.TermResponse;
import com.zipdamember.global.error.custom.business.NotFoundResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TermService {

    private final TermRepository termRepository;

    public List<TermResponse> showTerms() {
        return termRepository.findAllBy().orElseThrow(() -> new NotFoundResourceException("활성 약관이 존재하지 않습니다."));
    }
}
