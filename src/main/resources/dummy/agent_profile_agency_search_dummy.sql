-- 관리자 중개소 검색·조회 화면용 더미 데이터
-- 생성 건수: 50건 (ACTIVE 17건 / SUSPENDED 17건 / CLOSED 16건)
-- 재실행해도 agent_id 또는 사업자등록번호가 같은 행은 추가하지 않습니다.
-- member_id는 중개소 검색·조회 API에서 조인하지 않는 논리 참조값입니다.

INSERT INTO agent_profile (
    agent_id,
    member_id,
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
    updated_at
)
SELECT
    seed.agent_id,
    seed.member_id,
    seed.approved_at,
    seed.business_registration_no,
    seed.agency_name,
    seed.representative_name,
    seed.phone,
    seed.address,
    seed.operating_status,
    NULL,
    seed.status_changed_at,
    seed.approved_at,
    seed.status_changed_at
FROM (
    SELECT
        sequence_no,
        990000000000001000 + sequence_no AS agent_id,
        990000000000002000 + sequence_no AS member_id,
        DATE_ADD('2026-07-01 09:00:00', INTERVAL sequence_no DAY) AS approved_at,
        CONCAT('900-', LPAD(sequence_no, 2, '0'), '-', LPAD(sequence_no, 5, '0')) AS business_registration_no,
        CONCAT(
            CASE MOD(sequence_no - 1, 10)
                WHEN 0 THEN '강남'
                WHEN 1 THEN '송파'
                WHEN 2 THEN '마포'
                WHEN 3 THEN '수성'
                WHEN 4 THEN '해운대'
                WHEN 5 THEN '분당'
                WHEN 6 THEN '광명'
                WHEN 7 THEN '동탄'
                WHEN 8 THEN '일산'
                ELSE '세종'
            END,
            '테스트공인중개사사무소',
            LPAD(sequence_no, 2, '0')
        ) AS agency_name,
        CONCAT('테스트대표', LPAD(sequence_no, 2, '0')) AS representative_name,
        CONCAT('010-7000-', LPAD(sequence_no, 4, '0')) AS phone,
        CONCAT(
            CASE MOD(sequence_no - 1, 5)
                WHEN 0 THEN '서울특별시 강남구'
                WHEN 1 THEN '서울특별시 송파구'
                WHEN 2 THEN '서울특별시 마포구'
                WHEN 3 THEN '대구광역시 수성구'
                ELSE '부산광역시 해운대구'
            END,
            ' 테스트로 ',
            sequence_no
        ) AS address,
        CASE
            WHEN sequence_no <= 17 THEN 'ACTIVE'
            WHEN sequence_no <= 34 THEN 'SUSPENDED'
            ELSE 'CLOSED'
        END AS operating_status,
        DATE_ADD('2026-08-20 10:00:00', INTERVAL sequence_no HOUR) AS status_changed_at
    FROM (
        SELECT ones.number + tens.number * 10 + 1 AS sequence_no
        FROM (
            SELECT 0 AS number UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
        ) AS ones
        CROSS JOIN (
            SELECT 0 AS number UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        ) AS tens
    ) AS sequence
) AS seed
WHERE NOT EXISTS (
    SELECT 1
    FROM agent_profile existing_profile
    WHERE existing_profile.agent_id = seed.agent_id
       OR existing_profile.business_registration_no = seed.business_registration_no
)
ORDER BY seed.sequence_no;
