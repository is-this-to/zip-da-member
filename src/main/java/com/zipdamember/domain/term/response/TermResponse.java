package com.zipdamember.domain.term.response;

import com.zipdamember.domain.term.entity.Term;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "활성 약간 조회")
public record TermResponse(
        String termId,
        String termType,
        String termVersion,
        String title,
        String content,
        Boolean isRequired
) {
    public static TermResponse from(Term term) {
        return new TermResponse(term.getTermId().toString(), term.getTermType(), term.getTermVersion(), term.getTitle(), term.getContent(), term.getIsRequired());
    }
}
