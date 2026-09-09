package com.zipdamember.domain.admin.constant;

public enum AdminAuditAction {
    ADMIN_ACCOUNT_CREATE("관리자 계정 생성"),
    ADMIN_ROLE_ASSIGN("관리자 역할 부여"),
    ADMIN_ROLE_REVOKE("관리자 역할 회수"),
    MEMBER_PRIVATE_DATA_VIEW("회원 개인정보 원문 확인"),
    MEMBER_SUSPEND("회원 정지"),
    MEMBER_SUSPEND_RELEASE("회원 정지 해제"),
    AGENT_DOCUMENT_VIEW("중개사 서류 열람"),
    AGENT_APPLICATION_SUPPLEMENT_REQUEST("중개사 신청 보완 요청"),
    AGENT_APPLICATION_APPROVE("중개사 신청 승인"),
    AGENT_APPLICATION_REJECT("중개사 신청 반려"),
    PROPERTY_HIDE_REQUEST("매물 숨김 요청"),
    PROPERTY_RESTORE_REQUEST("매물 복구 요청");

    private final String description;

    AdminAuditAction(String description) {
        this.description = description;
    }
}
