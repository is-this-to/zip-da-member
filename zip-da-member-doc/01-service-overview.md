# Member 서비스 개요

[README로 돌아가기](../README.md)

## 1. 서비스 소개

`zip-da-member`는 회원 계정, 인증, 권한, 중개사 전환, 관리자 계정을 담당하는 Spring Boot 서비스이다.

- 회원·관리자 클라이언트의 일반 API 요청은 Gateway(`zip-da-scg`)를 거친다.
- Member는 계정과 권한의 기준 데이터를 관리한다.
- Property 등 다른 서비스는 필요한 회원 권한을 Member의 내부 API로 조회할 수 있다.

이 문서는 Member 서비스의 큰 그림을 설명한다. 데이터 관계와 컬럼은 [ERD & 데이터베이스](02-erd-and-database.md), 구현 범위와 계층별 책임은 [아키텍처](03-architecture.md), 전체 API 경로와 DTO는 [API 명세](04-api-specification.md)에 정리되어 있다.

## 2. 주요 기능

| 영역 | 기능 | 대표 경로 |
| --- | --- | --- |
| 회원·인증 | 회원가입, 이메일 로그인·로그아웃, Refresh Token 재발급, 비밀번호 재설정 | `POST /api/member`, `POST /api/member/auth/sessions`, `POST /api/member/auth/token-refreshes` |
| 소셜 인증 | Kakao OAuth2 로그인, 추가 정보 가입, 기존 계정 연결 | `/api/auth/oauth2/**`, `/api/member/auth/social-signups` |
| 프로필·파일 | 내 프로필과 이미지 관리, 공개 파일과 비공개 증빙 문서 접근 | `GET /api/member/members/me`, `/api/member/files/**` |
| 중개사 | 전환 신청, 증빙 서류 업로드·OCR, 외부 기관 검증, 공개 프로필 | `/api/member/agent-applications/**`, `/api/member/agents/**` |
| 관리자 | 회원 제재, 중개사 신청 심사, 관리자 계정·역할과 감사 이력 | `/api/member/admin/**` |
| 내부 연동 | 다른 서비스에 회원 행위 허용 여부 제공 | `GET /internal/members/{memberId}/permissions` |

위 경로는 기능을 파악하기 위한 대표 예시이다. 실제 메서드, 요청·응답 DTO, 역할 조건은 [API 명세](04-api-specification.md)와 Controller 구현을 기준으로 확인해야 한다. 중개사와 관리자 업무의 상태 변경 흐름은 [주요 기능](06-key-features.md)에 정리되어 있다.

## 3. 기술 스택

| 영역 | 현재 구성 |
| --- | --- |
| 언어·실행 환경 | Java 21 |
| 애플리케이션 | Spring Boot 4.1.1, Spring MVC, Spring Security |
| 데이터 | Spring Data JPA, QueryDSL 5.1, MySQL |
| 인증 | OAuth2 Client, JJWT 0.12.6 |
| 파일·외부 연동 | MinIO, Google Cloud Vision/Storage, 메일, NTS/MOLIT |
| API 문서·빌드 | springdoc-openapi 3.0.3, Gradle Wrapper |

## 4. 전체 요청 처리 흐름

```text
Client / Admin Client
    │ 일반 API 요청, 보호된 요청에는 Access Token
    ▼
API Gateway (zip-da-scg)
    │ 경로 라우팅, Access Token 검증 후 X-User-* 헤더 생성
    ▼
Member Filter (TraceIdFilter → HeaderAuthenticationFilter)
    │ 요청 추적 ID, 내부 사용자 헤더로 Spring Authentication 구성
    ▼
Controller (@Valid, @PreAuthorize)
    │ HTTP 계약·입력값·역할 검사
    ▼
Service (트랜잭션, 소유권·상태 등 업무 규칙)
    ├─ Repository (Spring Data JPA / QueryDSL) → Member MySQL
    └─ 필요 시 파일 저장소·메일·Kakao·외부 검증 API
    │
    ▼
GlobalResponseDTO(code, message, data, traceId) → Client
```

Member의 `HeaderAuthenticationFilter`는 JWT를 직접 검증하지 않는다. Gateway가 전달한 `X-User-Id`, `X-User-Role`을 Spring 인증 정보로 바꾼다.

HTTP 보안 설정은 요청을 `permitAll()`로 통과시킨다. 접근 가능 여부는 Controller의 `@PreAuthorize`와 Service의 업무 조건으로 판단한다.

따라서 Member 직접 접근을 제한해야 한다. Gateway는 외부에서 보낸 `X-User-*` 헤더를 제거해야 한다. 인증·재발급의 상세 흐름은 [JWT 인증 가이드](05-jwt-authentication.md)에 정리되어 있다.

## 5. 공통 응답 형식

일반적인 Controller 정상 응답은 `GlobalResponseDTO<T>`로 감싼다. 오류는 `GlobalExceptionHandler`가 같은 필드 구조로 변환하며, `TraceIdFilter`는 요청 추적 ID를 응답 헤더와 로그에도 연결한다.

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {},
  "traceId": "request-trace-id"
}
```

| 필드 | 의미 |
| --- | --- |
| `code` | 처리 결과 코드 |
| `message` | 결과 메시지 |
| `data` | 응답 DTO. 값이 없으면 `null`일 수 있음 |
| `traceId` | 요청·응답·로그를 연결하는 추적 ID |

로컬 실행과 OpenAPI 확인 방법은 [개발 환경 세팅](07-development-setup.md)에 정리되어 있다.
