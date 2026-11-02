# JWT 인증 가이드

[README로 돌아가기](../README.md)

## 1. 토큰 구성

| 구분 | Access Token | Refresh Token |
| --- | --- | --- |
| 회원 만료 | 15분 | 7일 |
| 관리자 만료 | 10분 | 7일 |
| 전달 | `Authorization: Bearer` | `HttpOnly` Cookie |
| 회원 Cookie | - | `user-refresh-token`, Path `/api/member/auth` |
| 관리자 Cookie | - | `admin-refresh-token`, Path `/api/member/admin/auth` |

공통 표준 JWT 필드(registered claim)는 `sub`, `iss`, `jti`, `iat`, `exp`이다. 모든 발급 토큰에 UUID `jti`가 포함된다.

- 회원: `type=MEMBER`, `tokenType=ACCESS|REFRESH`, `role=USER|AGENT`
- 관리자 Access: `type=ADMIN`, `tokenType=ACCESS`, `roles=[...]`
- 관리자 Refresh: `type=ADMIN`, `tokenType=REFRESH`

## 2. 로그인과 재발급 흐름

```text
Browser
   │ POST /api/member/auth/sessions (email, password)
   ▼
Gateway ───────────────────────────────▶ Member Service
                                           │ Access/Refresh Token 발급
                                           │ login_session 저장
   ◀───────────────────────────────────────┘
   │ Access Token 응답 + Set-Cookie(Refresh Token)
   │
   │ 보호 API + Authorization: Bearer <access-token>
   ▼
Gateway
   │ Access Token 검증, X-User-* 생성
   ▼
Member Service ──▶ 보호 API 처리

Browser
   │ POST /api/member/auth/token-refreshes + Refresh Cookie
   ▼
Gateway ──▶ Member Service ──▶ login_session 잠금·상태 확인
   ▲                              │ Refresh Token 회전
   └──────────────────────────────┘ 새 Access Token + Set-Cookie
```

재발급 시 Member는 JWT 서명과 필드(claim), DB 세션의 사용자 일치, 만료·폐기 여부, 회원 활성 상태를 확인하고 Refresh Token을 새 값으로 교체한다. 관리자도 현재 활성 역할을 다시 조회해 Access Token에 반영한다.

## 3. Gateway 연동 규칙

Member의 `HeaderAuthenticationFilter`는 JWT 자체를 해석하지 않는다. Gateway는 다음을 보장해야 한다.

1. 외부 `X-User-*` 헤더를 제거한다.
2. JWT 서명, 발급자(issuer), `exp`, `type`, `tokenType`을 검증한다.
3. 회원 `sub/role`, 관리자 `sub/roles`를 `X-User-Id`, `X-User-Type`, `X-User-Role`로 변환한다.
4. 다중 관리자 역할은 쉼표로 구분해 전달한다.
5. Cookie와 `Set-Cookie`를 변경하거나 누락하지 않는다.

## 4. 인증 주체별 책임

JWT 인증은 브라우저, Gateway, Member 서비스가 역할을 나누어 처리한다.

| 주체 | 책임 |
| --- | --- |
| Browser | 로그인 응답의 Access Token으로 보호 API 호출, Refresh Cookie를 포함한 재발급 요청, 로그아웃 후 인증 상태 제거 |
| Gateway | Access Token의 서명·issuer·만료·토큰 유형 검증, 외부 `X-User-*` 제거, 검증된 사용자 ID와 역할 헤더 생성 |
| Member | 로그인 자격 증명 확인, Access/Refresh Token 발급, Refresh Session 저장·잠금·회전·폐기, 최신 회원 상태와 관리자 역할 확인 |
| MySQL | 회원·관리자 Refresh Session의 만료 및 폐기 상태 보관 |

Access Token은 보호 API의 짧은 인증 수단이고, Refresh Token은 새 Access Token을 발급받기 위한 세션 자격 증명이다. 브라우저는 Refresh Token 값을 직접 읽지 않고 Cookie 전송만 담당한다.

## 5. 토큰 상태별 처리

| 상황 | 처리 결과 |
| --- | --- |
| Access Token 정상 | Gateway가 사용자 헤더를 생성하고 Member 보호 API 호출 |
| Access Token 만료 | 보호 API를 처리하지 않고 재발급 흐름으로 전환 |
| Refresh Token 정상 | DB Session과 계정 상태 확인 후 Access/Refresh Token 교체 |
| Refresh Token 만료·폐기 | 재발급 거부, Browser 인증 상태 종료 |
| 회원 상태 비활성 | 새 토큰 발급 거부 |
| 관리자 역할 없음 | 관리자 토큰 재발급 거부 |
| 비밀번호 변경 | 회원의 활성 로그인 Session 폐기 |

여러 보호 API가 동시에 인증 만료를 감지하더라도 하나의 재발급 결과를 공유해야 한다. 이는 특정 JavaScript 코드가 아니라 클라이언트 인증 상태 관리 계층이 지켜야 하는 동시 요청 처리 규칙이다.
