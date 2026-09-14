-- 외부 API 검증 스케줄러 테스트 회원
INSERT INTO member_account (
    member_id,
    email,
    password,
    name,
    nickname,
    phone,
    status,
    member_role,
    email_verification_at,
    created_at,
    updated_at
)
SELECT
    990000000000000002,
    'agent-verification-test@zipda.local',
    NULL,
    'API검증테스트',
    'test',
    '010-0000-0000',
    'ACTIVE',
    'USER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM member_account
    WHERE member_id = 990000000000000002
)
AND NOT EXISTS (
    SELECT 1
    FROM member_account
    WHERE email = 'agent-verification-test@zipda.local'
       OR nickname = 'test'
);

-- 외부 API 검증 스케줄러 대상 중개사 전환 신청
INSERT INTO agent_application (
    application_id,
    member_id,
    status,
    submitted_at,
    request_business_no,
    request_agency_registration_no,
    request_agency_name,
    request_start_date,
    request_representative_name,
    created_at,
    updated_at
)
SELECT
    990000000000000001,
    990000000000000002,
    'PENDING',
    CURRENT_TIMESTAMP,
    '8500803204',
    '27260-2026-00021',
    '하임공인중개사사무소',
    '2026-02-25',
    '이누리',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE EXISTS (
    SELECT 1
    FROM member_account
    WHERE member_id = 990000000000000002
)
AND NOT EXISTS (
    SELECT 1
    FROM agent_application
    WHERE application_id = 990000000000000001
);
