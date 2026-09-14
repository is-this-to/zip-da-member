package com.zipdamember.domain.member.service;

import com.querydsl.core.BooleanBuilder;
import com.zipdamember.domain.member.entity.QMemberAccount;
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
        QMemberAccount member = QMemberAccount.memberAccount;
        BooleanBuilder condition = new BooleanBuilder();

        if (request.email() != null) {
            condition.and(member.email.like(likePattern(request.email()), '!')); // 이메일 부분일치
        }
        if (request.nickname() != null) {
            condition.and(member.nickname.like(likePattern(request.nickname()), '!')); // 닉네임 부분일치
        }
        if (request.status() != null) {
            condition.and(member.status.eq(request.status())); // 회원 상태
        }
        if (request.role() != null) {
            condition.and(member.memberRole.eq(request.role())); // 회원 역할
        }

        PageRequest pageable = PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(
                        Sort.Order.desc("createdAt"), // 가입일 최신순
                        Sort.Order.desc("memberId")   // 동일 가입일 ID 내림차순
                )
        );

        return AdminMemberListResponse.from(
                adminMemberRepository.findAll(condition, pageable) // 조건·페이징 조회
                        .map(AdminMemberResponse::from)
        );
    }

    private String likePattern(String value) {
        return "%" + value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_") + "%";
    }
}
