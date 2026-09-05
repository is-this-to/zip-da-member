package com.zipdamember.domain.member.service;

import com.zipdamember.domain.member.repository.AdminMemberRepository;
import com.zipdamember.domain.member.request.AdminMemberSearchRequest;
import com.zipdamember.domain.member.response.AdminMemberListResponse;
import com.zipdamember.domain.member.response.AdminMemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminMemberService {
    private final AdminMemberRepository adminMemberRepository;

    @Transactional(readOnly = true)
    public AdminMemberListResponse search(AdminMemberSearchRequest request) {
        PageRequest pageable = PageRequest.of(request.page(), request.size(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("memberId")));
        String keyword = request.keyword();
        String pattern = keyword == null ? null
                : "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return AdminMemberListResponse.from(adminMemberRepository
                .findMembers(pattern, request.status(), request.role(), pageable)
                .map(AdminMemberResponse::from));
    }
}
