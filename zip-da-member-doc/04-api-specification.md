# Member API 명세

[README로 돌아가기](../README.md)

이 문서는 현재 Controller의 경로 설정을 기준으로 한 탐색용 명세이다. 요청·응답 DTO와 오류 코드의 최종 기준은 실행 중 생성되는 `/api-docs`이다.

## 공통 계약

- 외부 호출은 Gateway 주소와 `/api/member/**`를 사용한다.
- 보호 API는 Gateway에 `Authorization: Bearer <access-token>`을 보냅니다.
- `USER`, `AGENT`, 관리자 역할은 Spring Security에서 `ROLE_` 접두어를 붙여 평가한다.
- ID는 JavaScript 숫자 정밀도 손실을 피하도록 클라이언트에서 문자열로 취급한다.
- 응답은 `code`, `message`, `data`, `traceId`를 담은 공통 형식이다.

## Trace ID 사용 예시

호출자는 영문, 숫자, `.`, `_`, `-`로 구성된 1~100자의 `X-Trace-Id`를 전달할 수 있다. 값이 없거나 형식이 잘못되면 Member가 새 ID를 생성한다.

```http
GET /api/member/members/me HTTP/1.1
Authorization: Bearer <access-token>
X-Trace-Id: web-20260930-a1b2c3
```

동일한 값은 응답 헤더와 본문(body)에 포함된다.

```http
HTTP/1.1 200 OK
X-Trace-Id: web-20260930-a1b2c3
Content-Type: application/json
```

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {
    "memberId": "123456789"
  },
  "traceId": "web-20260930-a1b2c3"
}
```

로그 진단 정보(MDC)에도 같은 값이 들어가므로 Client → Gateway → Member의 요청을 연계해 찾을 수 있다.

```text
2026-09-30T10:15:30.123Z INFO [traceId=web-20260930-a1b2c3] ...
```

Gateway가 외부 `X-Trace-Id`를 그대로 유지할지 재생성할지는 전체 시스템 정책으로 정해야 하며, 실제 비밀값이나 개인정보를 추적 ID에 넣으면 안 된다.

## 공개·인증 API

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| POST | `/api/member/member-validations` | 이메일/닉네임 중복 검사 | 공개 |
| POST | `/api/member/email-verifications` | 가입용 인증 메일 발송 | 비로그인 |
| PATCH | `/api/member/email-verifications/{verificationId}` | 가입용 인증번호 확인 | 비로그인 |
| GET | `/api/member/terms` | 활성 약관 조회 | 공개 |
| POST | `/api/member` | 일반 회원가입 | 비로그인 |
| POST | `/api/member/auth/sessions` | 이메일 로그인 | 비로그인 |
| POST | `/api/member/auth/token-refreshes` | 회원 Refresh Cookie 회전 | Refresh Cookie |
| DELETE | `/api/member/auth/sessions/current` | 현재 회원 세션 로그아웃 | USER/AGENT + Cookie |
| POST | `/api/member/auth/password-reset-requests` | 재설정 링크 요청 | 비로그인 |
| POST | `/api/member/auth/password-resets` | 비밀번호 재설정 | 비로그인 + reset token |
| GET | `/api/member/auth/social-signups/current` | OAuth2 추가 가입 context 조회 | 소셜 가입 Cookie |
| POST | `/api/member/auth/social-signups` | Kakao 신규 회원가입 | 소셜 가입 Cookie |
| POST | `/api/member/auth/social-links` | 기존 일반 계정에 Kakao 연결 | 소셜 가입 Cookie + 재인증 |
| GET | `/api/auth/oauth2/authorization/kakao` | Kakao 인증 시작 | 공개 |
| GET | `/api/auth/oauth2/callback/kakao` | Kakao callback | OAuth2 provider |

## 프로필·파일 API

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| GET | `/api/member/members/me` | 내 프로필 조회 | USER/AGENT |
| PATCH | `/api/member/members/me` | 내 프로필 부분 수정 | USER/AGENT |
| POST | `/api/member/members/me/password-verifications` | 비밀번호 변경 인증 메일 발송 | USER/AGENT |
| PATCH | `/api/member/members/me/password-verifications/{verificationId}` | 비밀번호 변경 인증번호 확인 | USER/AGENT |
| PATCH | `/api/member/members/me/password` | 비밀번호 변경 및 Refresh Cookie 제거 | USER/AGENT |
| POST | `/api/member/files/profiles` | 가입 전 프로필 이미지 업로드 | 공개 정책 확인 필요 |
| POST | `/api/member/files/profiles/me` | 내 프로필 이미지 업로드 | USER/AGENT |
| POST | `/api/member/files/agent-profiles/me` | 중개사 대표 이미지 업로드 | AGENT |

## 중개사 API

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| POST | `/api/member/agent-applications` | 전환 신청 초안 생성 | USER |
| GET | `/api/member/agent-applications/current` | 내 최근 신청 조회 | USER/AGENT |
| PATCH | `/api/member/agent-applications/{applicationId}` | OCR 확인 후 신청 정보 수정 | USER |
| POST | `/api/member/agent-applications/{applicationId}/documents` | 증빙 업로드와 OCR | USER, multipart |
| POST | `/api/member/agent-applications/{applicationId}/submissions` | 신청 제출/재제출 | USER |
| GET | `/api/member/agent-applications/{applicationId}/documents/{documentId}/download-url` | 내 서류 Signed URL | USER/AGENT |
| GET | `/api/member/agents/{agentId}` | 중개사 공개 프로필 | 공개 |
| PATCH | `/api/member/agents/{agentId}` | 중개사 공개 프로필 수정 | AGENT, 본인 확인 |

## 관리자 인증·계정 API

| Method | Path | 설명 | 권한 |
| --- | --- | --- | --- |
| POST | `/api/member/admin/auth/admin-sessions` | 관리자 로그인 | 비로그인 |
| DELETE | `/api/member/admin/auth/admin-sessions/current` | 관리자 로그아웃 | 관리자 + Cookie |
| POST | `/api/member/admin/auth/admin-token-refreshes` | 관리자 토큰 회전 | 관리자 Refresh Cookie |
| POST | `/api/member/admin/admins` | 관리자 계정 생성 | SUPER_ADMIN |
| GET | `/api/member/admin/admins` | 관리자/권한 검색 | SUPER_ADMIN |
| POST | `/api/member/admin/admins/{adminId}/roles` | 역할 부여 | SUPER_ADMIN |
| DELETE | `/api/member/admin/admins/{adminId}/roles/{roleCode}` | 역할 회수 | SUPER_ADMIN |

### 관리자 최초 비밀번호 변경 구현 확인 필요

`AdminAuthService`에는 최초 비밀번호 등록 메서드 `registerInitialPassword(...)`가 있다. 최초 로그인 응답은 `passwordChangeRequired=true`와 빈 Access Token을 반환한다.

현재 `AdminAuthController`에는 이 메서드를 호출하는 API 경로가 없다. 따라서 완성된 외부 기능으로 문서화하지 않는다.

관리자 클라이언트 연동 전에 다음 사항을 확정해야 한다.

- API 경로와 비로그인 상태의 인증 근거
- 요청 횟수 제한(rate limit)과 감사 정책

## 관리자 회원·중개사 API

| Method | Path | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | `/api/member/admin/members` | 회원 검색 | CS_ADMIN/SUPER_ADMIN |
| GET | `/api/member/admin/members/{memberId}/sanction-histories` | 제재 이력 | CS_ADMIN/SUPER_ADMIN |
| POST | `/api/member/admin/members/{memberId}/sanctions` | 회원 제재 | CS_ADMIN/SUPER_ADMIN |
| PATCH | `/api/member/admin/members/sanctions/{sanctionId}/release` | 제재 해제 | CS_ADMIN/SUPER_ADMIN |
| GET | `/api/member/admin/agent-applications` | 심사 목록 | SALES_ADMIN/SUPER_ADMIN |
| GET | `/api/member/admin/agent-applications/{applicationId}` | 신청·서류 상세 | SALES_ADMIN/SUPER_ADMIN |
| GET | `/api/member/admin/agent-applications/{applicationId}/documents/{documentId}/download-url` | 심사용 Signed URL | SALES_ADMIN/SUPER_ADMIN |
| PATCH | `/api/member/admin/agent-applications/{applicationId}/supplement-request` | 보완 요청 | SALES_ADMIN/SUPER_ADMIN |
| PATCH | `/api/member/admin/agent-applications/{applicationId}/approval` | 승인 | SALES_ADMIN/SUPER_ADMIN |
| GET | `/api/member/admin/agencies` | 중개소 검색 | CS/SALES/SUPER_ADMIN |
| GET | `/api/member/admin/agencies/operating-statuses` | 영업 상태 목록 | SALES_ADMIN/SUPER_ADMIN |
| PATCH | `/api/member/admin/agencies/{agentId}/operating-status` | 영업 상태 변경 | SALES_ADMIN/SUPER_ADMIN |

## 내부 API

### 매물 쓰기 권한 조회

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/internal/members/{memberId}/permissions?action={action}` | 매물 등록·수정 가능 여부 조회 |

Property 서비스는 매물 등록·수정 전에 Member 서비스에 이 API를 동기 호출한다. `action`에는 `PROPERTY_CREATE` 또는 `PROPERTY_UPDATE`를 사용하며, 현재 Member 서비스는 두 작업에 같은 권한 판정 정책을 적용한다.

`memberId`는 양의 정수 문자열이어야 하며, 지원하지 않는 `action` 또는 잘못된 `memberId` 형식은 권한 판정 전 잘못된 요청으로 처리한다.

**호출 예시**

```http
GET /internal/members/1001/permissions?action=PROPERTY_CREATE HTTP/1.1
Accept: application/json
X-Trace-Id: property-permission-20261020-001
```

**허용 응답 예시**

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {
    "allowed": true,
    "reason": null
  },
  "traceId": "property-permission-20261020-001"
}
```

**거부 응답 예시**

중개소가 휴업 또는 폐업 상태인 중개사가 매물 수정을 요청하면 Member는 HTTP 200 응답 안에 거부 결과를 반환한다.

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {
    "allowed": false,
    "reason": "AGENT_NOT_ACTIVE"
  },
  "traceId": "property-permission-20261020-001"
}
```

| 검사 대상 | `reason` |
| --- | --- |
| 회원 없음 | `MEMBER_NOT_FOUND` |
| 탈퇴 회원 | `MEMBER_WITHDRAWN` |
| 정지 회원 | `MEMBER_SUSPENDED` |
| 잠금 회원 | `MEMBER_LOCKED` |
| 활성 계정 제재 | `ACCOUNT_SANCTIONED` |
| 활성 매물 제재 | `PROPERTY_SANCTIONED` |
| AGENT 역할이지만 중개사 프로필 없음 | `AGENT_PROFILE_NOT_FOUND` |
| 중개사 프로필 삭제 또는 영업 상태가 `ACTIVE`가 아님 | `AGENT_NOT_ACTIVE` |

Property 서비스는 `allowed: false`를 `MEMBER_PERMISSION_DENIED`로, Member의 HTTP 오류·비정상 응답·통신 실패를 `MEMBER_API_UNAVAILABLE`로 변환해 매물 쓰기를 차단한다.

> **보안 유의 사항**
> - 이 경로는 외부 Gateway에서 제외해야 한다.
> - 현재 Member의 HTTP 보안 설정은 모든 경로를 `permitAll()`로 두고 Controller에도 별도 권한 제어가 없다. 따라서 배포 환경에서 네트워크 격리 또는 서비스 간 인증을 적용하기 전에는 외부에 노출하면 안 된다.

