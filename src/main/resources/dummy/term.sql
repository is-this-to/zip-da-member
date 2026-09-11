INSERT INTO term (
    term_id,
    term_type,
    term_version,
    title,
    content,
    is_required,
    status,
    created_at
) VALUES
    (
        1001,
        'SERVICE',
        '1.0',
        '서비스 이용약관',
        'ZIPDA 서비스 이용을 위한 약관입니다.',
        TRUE,
        TRUE,
        NOW()
    ),
    (
        1002,
        'PRIVACY',
        '1.0',
        '개인정보 수집 및 이용 동의',
        '서비스 제공을 위해 필요한 개인정보 수집 및 이용에 대한 안내입니다.',
        TRUE,
        TRUE,
        NOW()
    ),
    (
        1003,
        'MARKETING',
        '1.0',
        '마케팅 정보 수신 동의',
        '이벤트 및 혜택 안내를 위한 선택 동의입니다.',
        FALSE,
        TRUE,
        NOW()
    )
ON DUPLICATE KEY UPDATE
    term_type = VALUES(term_type),
    term_version = VALUES(term_version),
    title = VALUES(title),
    content = VALUES(content),
    is_required = VALUES(is_required),
    status = VALUES(status);
