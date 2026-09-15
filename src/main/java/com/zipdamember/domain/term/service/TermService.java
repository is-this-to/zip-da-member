package com.zipdamember.domain.term.service;

import com.zipdamember.domain.term.repository.TermRepository;
import com.zipdamember.domain.term.response.TermResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TermService {

    private final TermRepository termRepository;

    public List<TermResponse> showTerms() {
        return termRepository.findAllByStatusTrue()
                .stream()
                .map(TermResponse::from)
                .toList();
    }
}
