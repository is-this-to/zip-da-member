-- ZIPDA 마이페이지 / 중개사 공개 프로필 개발용 더미 데이터
-- MySQL 8.x 기준
--
-- 공통 로그인 비밀번호: test1234$%
-- 일반 회원: dummy.user@zipda.test
-- 중개사 회원: dummy.agent@zipda.test
-- 공개 중개사 프로필: GET /api/member/agents/920000000000000001
--
-- 주의: 로컬·개발 DB에서만 사용하세요.

START TRANSACTION;

INSERT INTO member_account (
    member_id,
    email,
    password,
    name,
    nickname,
    phone,
    status,
    profile_file_id,
    member_role,
    email_verification_at,
    withdrawn_at,
    created_at,
    updated_at
) VALUES
    (
        910000000000000001,
        'dummy.user@zipda.test',
        '$2b$12$rsGZPtjctbI6bSGzS4P3mOSdrABnJuHfnKxEQwvm4KFu72BN3XNKK',
        '집다회원',
        '집구경좋아',
        '01012345678',
        'ACTIVE',
        NULL,
        'USER',
        NOW(),
        NULL,
        NOW(),
        NOW()
    ),
    (
        910000000000000002,
        'dummy.agent@zipda.test',
        '$2b$12$rsGZPtjctbI6bSGzS4P3mOSdrABnJuHfnKxEQwvm4KFu72BN3XNKK',
        '김지민',
        '지민중개사',
        '01098765432',
        'ACTIVE',
        NULL,
        'AGENT',
        NOW(),
        NULL,
        NOW(),
        NOW()
    )
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    name = VALUES(name),
    nickname = VALUES(nickname),
    phone = VALUES(phone),
    status = VALUES(status),
    member_role = VALUES(member_role),
    email_verification_at = VALUES(email_verification_at),
    withdrawn_at = NULL,
    updated_at = NOW();

INSERT INTO agent_profile (
    agent_id,
    member_id,
    intro,
    profile_file_id,
    approved_at,
    business_registration_no,
    agency_name,
    representative_name,
    phone,
    address,
    operating_status,
    status_changed_by,
    status_changed_at,
    created_at,
    updated_at,
    deleted_at
) VALUES (
    920000000000000001,
    910000000000000002,
    '강남구와 서초구를 중심으로 주거용 부동산을 중개합니다. 고객의 조건을 꼼꼼히 확인하고 확인된 정보만 안내드리겠습니다.',
    NULL,
    NOW(),
    '1234567890',
    '집다 공인중개사사무소',
    '김지민',
    '025551234',
    '서울특별시 강남구 테헤란로 123, 4층',
    'ACTIVE',
    NULL,
    NULL,
    NOW(),
    NOW(),
    NULL
)
ON DUPLICATE KEY UPDATE
    member_id = VALUES(member_id),
    intro = VALUES(intro),
    approved_at = VALUES(approved_at),
    business_registration_no = VALUES(business_registration_no),
    agency_name = VALUES(agency_name),
    representative_name = VALUES(representative_name),
    phone = VALUES(phone),
    address = VALUES(address),
    operating_status = VALUES(operating_status),
    deleted_at = NULL,
    updated_at = NOW();

-- 재실행 시 같은 중개사의 하위 데이터가 중복되지 않도록 더미 행만 교체합니다.
DELETE FROM agent_specialty
WHERE agent_id = 920000000000000001;

INSERT INTO agent_specialty (
    specialty_id,
    agent_id,
    region_code,
    region_name,
    display_order
) VALUES
    (930000000000000001, 920000000000000001, '1168010100', '강남구 역삼동', 0),
    (930000000000000002, 920000000000000001, '1165010800', '서초구 서초동', 1),
    (930000000000000003, 920000000000000001, '1171010400', '송파구 송파동', 2);

DELETE FROM agent_business_hour
WHERE agent_id = 920000000000000001;

-- day_of_week: 월=1, 화=2, 수=3, 목=4, 금=5, 토=6, 일=7
INSERT INTO agent_business_hour (
    business_hour_id,
    agent_id,
    day_of_week,
    open_time,
    close_time,
    closed
) VALUES
    (940000000000000001, 920000000000000001, 1, '09:30:00', '18:30:00', FALSE),
    (940000000000000002, 920000000000000001, 2, '09:30:00', '18:30:00', FALSE),
    (940000000000000003, 920000000000000001, 3, '09:30:00', '18:30:00', FALSE),
    (940000000000000004, 920000000000000001, 4, '09:30:00', '18:30:00', FALSE),
    (940000000000000005, 920000000000000001, 5, '09:30:00', '18:30:00', FALSE),
    (940000000000000006, 920000000000000001, 6, '10:00:00', '15:00:00', FALSE),
    (940000000000000007, 920000000000000001, 7, NULL, NULL, TRUE);

COMMIT;
