# Member 서비스 아키텍처와 MSA 연동

[README로 돌아가기](../README.md)

## 1. ZIPDA 시스템 내 Member 서비스의 위치와 MSA 경계

이 문서에서 MSA는 각 서비스가 자신의 데이터와 업무 정책을 소유하고, API·이벤트로만 협업하는 구조를 뜻한다. `zip-da-member`는 ZIPDA MSA에서 신원·권한 기준 데이터를 관리한다. 상세 업무 기능과 상태 전이는 [주요 기능](06-key-features.md), API 계약은 [API 명세](04-api-specification.md)를 기준으로 한다.

- 데이터 소유권: 서비스별 DB를 해당 서비스가 관리.
- 현재 회원 권한 조회: 내부 API.
- `<V3예정>` 서비스 연동: 업무별 후속 처리 정책을 우선 확정하고, 상태 변경 이벤트는 보조 연동으로 검토.

```text
Client / Admin Client
        │ 외부 요청
        ▼
SCG ── /api/member/** 라우팅 ──▶ Member ──▶ Member DB
                                    ▲
Property ── 회원 권한 동기 조회 ──────┘

<V3 예정>: 회원 탈퇴·제재·중개소 상태 수명주기 및 서비스 간 후속 처리
           └─ <Kafka> 상태 변경 전달(부가 연동)
```

| 서비스 | 책임·소유 데이터 | Member와의 경계 |
| --- | --- | --- |
| SCG | 외부 요청 라우팅 · Access Token 검증 · 내부 사용자 헤더 구성 | 인증 정보 전달<br>외부 헤더·내부 경로 접근 검증: 5.1절 |
| Member | 회원·관리자 계정 · 회원 상태·제재 · 중개사 전환·영업상태 | Member DB 관리<br>회원 행위 허용 여부 제공 |
| Property | 매물·매물 관련 업무 데이터 | 매물 등록 및 일반 회원·중개사 수정 시 Member 권한 API 동기 호출<br>Member의 Property DB 직접 변경 없음 |


## 2. 서비스 내부 구조

```text
┌─────────────────────────────────────────────┐
│ Client / Admin Client / Internal Service    │
└─────────────────────┬───────────────────────┘
                      │ HTTP 요청
┌─────────────────────▼───────────────────────┐
│ Filter Layer                                │
│ TraceIdFilter → HeaderAuthenticationFilter  │
└─────────────────────┬───────────────────────┘
                      │ 인증 정보
┌─────────────────────▼───────────────────────┐
│ Controller Layer                            │
│ 요청 수신 · Bean Validation · @PreAuthorize │
└─────────────────────┬───────────────────────┘
                      │ DTO
┌─────────────────────▼───────────────────────┐
│ Service Layer                               │
│ 트랜잭션 · 업무 규칙 · 상태 변경            │
└──────────────┬──────────────────┬───────────┘
               │                  │
┌──────────────▼──────────────┐  ┌▼──────────────────────────┐
│ Repository Layer            │  │ External Integration      │
│ Spring Data JPA · QueryDSL  │  │ MinIO · Mail · Kakao      │
└──────────────┬──────────────┘  │ NTS · MOLIT · Vision      │
               │                 └───────────────────────────┘
┌──────────────▼──────────────┐
│ Member MySQL               │
└─────────────────────────────┘

예외 발생 ──▶ GlobalExceptionHandler ──▶ GlobalResponseDTO
```

패키지 구분: 업무 기능별 `domain/{admin,agent,auth,file,member,term,verification}` · 공통 기능 `global`.

- Controller: HTTP 요청·응답과 인증 사용자 식별
- Service: 트랜잭션, 업무 규칙, 상태 변경
- Repository: DB 접근

중개사 전환과 같은 개별 업무의 상태 전이·검증 규칙은 [주요 기능](06-key-features.md), 요청·응답 타입은 [API 명세](04-api-specification.md)에 정리한다.

## 3. 패키지 구조

아래 구조: `src/main/java/com/zipdamember`의 주요 디렉터리·대표 파일.
전체 파일 목록은 제외.

```text
src/main/java/com/zipdamember/
├── ZipDaMemberApplication.java
├── domain/                              # 업무 기능별 코드
│   ├── admin/                           # 관리자 계정·역할·감사 이력
│   │   ├── controller/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   ├── agent/                           # 중개사 신청·심사·프로필
│   │   ├── client/                      # 외부 자격 검증 연동
│   │   ├── controller/
│   │   ├── ocr/
│   │   ├── repository/
│   │   ├── scheduler/
│   │   └── service/
│   ├── auth/                            # 로그인·토큰 재발급·소셜 인증
│   │   ├── controller/AuthController.java
│   │   ├── entity/LoginSession.java
│   │   ├── repository/LoginSessionRepository.java
│   │   └── service/AuthService.java
│   ├── file/                            # 파일 메타데이터·업로드
│   ├── member/                          # 회원 프로필·상태·제재
│   ├── term/                            # 약관
│   └── verification/                    # 이메일 인증·중복 검사
│       ├── controller/VerificationController.java
│       ├── repository/VerificationMemberRepository.java
│       ├── scheduler/EmailVerificationCleanupScheduler.java
│       └── service/VerificationService.java
└── global/                              # 여러 업무 영역에서 쓰는 공통 코드
    ├── config/                          # JPA·MinIO·OAuth2·OpenAPI 등 설정
    ├── context/TraceIdContext.java
    ├── error/GlobalExceptionHandler.java
    ├── filter/TraceIdFilter.java
    ├── jpa/SoftDeleteFilterAspect.java
    ├── jwt/JwtProvider.java
    ├── response/GlobalResponseDTO.java
    └── security/filter/HeaderAuthenticationFilter.java

src/main/resources/
├── application.yaml
├── application-prod.yaml
└── dummy/                               # 기본 설정에서 실행되는 초기 데이터 SQL
```

`domain`: 업무 기능별 코드.
`global`: 추적 ID · 응답 · 보안 · 설정 등 공통 코드.
DB 접근: Repository · JPA/QueryDSL.

## 4. 공통 처리

### 4.1. 공통 응답

공통 형식: `GlobalResponseDTO<T>`의 `code` · `message` · `data` · `traceId`.
아래 요청은 이메일 중복 검사와 유효한 추적 ID의 예시이다.

```java
public record GlobalResponseDTO<T>(
        String code, String message, T data, String traceId
) { /* success(...), from(...) 팩터리 메서드 */ }
```

```http
POST /api/member/member-validations HTTP/1.1
Content-Type: application/json
X-Trace-Id: member-doc-example-001

{
  "typePolicy": "EMAIL",
  "value": "example@example.com"
}
```

#### 4.1.1. 정상 응답

응답 코드·메시지: `CustomResponseCode.SUCCESS`의 `00` · `SUCCESS`.
`available` 값: 실제 DB 조회 결과에 따라 결정.

```http
HTTP/1.1 200 OK
X-Trace-Id: member-doc-example-001
Content-Type: application/json
```

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {
    "available": true,
    "field": "EMAIL"
  },
  "traceId": "member-doc-example-001"
}
```

#### 4.1.2. 오류 응답

예시 조건: 필수 필드 `typePolicy` 누락.
처리: `@Valid` 검증 오류 → `GlobalExceptionHandler`.
응답: `data`는 `null` · 상세 필드 오류는 본문에 미포함.

```http
HTTP/1.1 400 Bad Request
X-Trace-Id: member-doc-example-001
Content-Type: application/json
```

```json
{
  "code": "E21",
  "message": "INVALID_PARAMETER_ERROR",
  "data": null,
  "traceId": "member-doc-example-001"
}
```

### 4.2. 요청 추적

`TraceIdFilter`: 모든 HTTP 요청의 `X-Trace-Id` 검증.
헤더 부재·형식 오류: 새 UUID 생성.
선택된 ID: 응답 헤더·로그 진단 정보(MDC)에 설정. 요청 종료 시 MDC 정리.

| 구현 | 기능 |
| --- | --- |
| `TraceIdFilter` | `X-Trace-Id` 검증·생성, 응답 헤더와 MDC 설정·정리 |
| `TraceIdContext` | 현재 요청의 trace ID 조회와 생성 |
| `GlobalExceptionHandler` | Business/Validation/System 예외를 공통 응답으로 변환 |
| `GlobalResponseDTO` | 정상·오류 응답의 공통 형식 제공 |

#### 4.2.1. traceId 생성·전파 규칙

traceId: `UUID.randomUUID().toString()`으로 생성하는 요청 식별자.

1. **Gateway 수신**: 양끝 공백 제거 · 영문·숫자·`.`, `_`, `-`의 1~100자 검사. 유효한 값 재사용, 그 외 새 UUID 생성.
2. **Gateway 전달**: 선택한 ID를 Member 요청·클라이언트 응답 헤더에 설정.
3. **Member 처리**: 유효한 ID 재사용 · 직접 요청의 누락·형식 오류 시 새 UUID 생성 · 요청 속성·MDC·응답 헤더·본문 `traceId`에 설정 · 종료 시 MDC 정리.

유효한 헤더 예시: `X-Trace-Id: member-doc-example-001`.
새 ID 형식 예시: `550e8400-e29b-41d4-a716-446655440000`.

## 5. 인증·인가 아키텍처

인증·인가 경로: Gateway → Security Filter → Method Security → Service.

| 구현 | 기능 |
| --- | --- |
| Gateway | Access Token 검증 후 내부 사용자 헤더 생성 |
| `HeaderAuthenticationFilter` | `X-User-Id`, `X-User-Role`을 Spring `Authentication`으로 변환 |
| `@PreAuthorize` | 해당 어노테이션이 있는 Controller 메서드의 역할 검사 |
| Service | 리소스 소유권, 계정 상태, 신청 상태 등 업무 권한 확인 |

HTTP 경로 설정: `SecurityConfiguration`의 `anyRequest().permitAll()`.
메서드 권한 검사: `@EnableMethodSecurity` · Controller의 `@PreAuthorize`.
`@PreAuthorize`가 없는 API에는 해당 역할 검사가 자동 적용되지 않는다.

`HeaderAuthenticationFilter`는 JWT를 직접 검증하지 않고 Gateway가 전달한 역할을 Spring 권한으로 변환한다. 토큰·Cookie 계약은 [JWT 인증 가이드](05-jwt-authentication.md), 사용자·관리자별 접근 범위와 업무 권한 적용 순서는 [주요 기능의 RBAC 업무 매트릭스](06-key-features.md#7-rbac-업무-매트릭스)를 기준으로 한다.

### 5.1. Gateway와 내부 인증 헤더 처리

1. **토큰 없음**
   - SCG 요청 전달 · 외부 `X-User-*` 헤더 제거 미보장
2. **토큰 있음**
   - 검증된 claim 기반 `X-User-*` 구성 · 동일 이름의 외부 헤더 처리 검증 필요
3. **Member 인증**
   - 전달된 헤더로 `Authentication` 생성 · 직접 접근 제한·네트워크 정책 확인 필요
4. **내부 API**
   - SCG 라우팅: `/api/member/**` · `/internal/**` 직접 접근 차단·서비스 간 인증 검증 필요
   - 내부 권한 Controller의 별도 접근 제어 annotation 없음

## 6. 논리 삭제 조회 정책

일반 조회: Hibernate Filter와 AOP로 논리 삭제 데이터 제외.
관리자 예외 조회: 필요한 Repository 메서드에서만 삭제 데이터 포함.

설정 위치: 엔티티의 Hibernate `@FilterDef`·`@Filter`, 요청 진입점의 `SoftDeleteFilterAspect`.
별도 환경 변수 없음.

### 6.1. 일반 조회

```text
RestController
      │ 메서드 진입
      ▼
SoftDeleteFilterAspect (@Before)
      │ Hibernate Session.enableFilter("softDelete")
      ▼
Service → Repository
      │ 일반 조회
      ▼
Hibernate Session
      │ withdrawn_at / deleted_at / status 조건 적용
      ▼
MySQL
```

기본 조회 조건:

- 회원: `withdrawn_at IS NULL`
- 관리자·역할·중개사 관련 데이터: `deleted_at IS NULL`
- 약관: `status = true`

탈퇴 데이터 포함 조회:

- 해당 Repository 메서드의 `@JPAWithDeleted`로 필터 비활성화.

```java
@JPAWithDeleted
@Override
Page<MemberAccount> findAll(Predicate predicate, Pageable pageable);
```

`JPAWithDeletedAspect`: 실행 전 필터 상태 저장 · 조회 종료 후 `finally`에서 기존 상태 복원.

`SoftDeleteFilterAspect`의 적용 범위: `@RestController` 진입점.
Scheduler 등 비 HTTP 경로: 삭제 데이터 포함 여부 별도 확인 필요.

## 7. 데이터와 외부 연동

테이블 관계·컬럼 명세: [ERD & 데이터베이스](02-erd-and-database.md).

- MySQL: 도메인과 로그인 세션의 원본 데이터
- MinIO: 프로필 및 중개사 신청 문서
- Google Cloud Vision/Storage: 이미지·PDF OCR
- NTS/MOLIT: 사업자와 중개사무소 정보 검증
- Kakao: OAuth2 인증
- Gmail SMTP: 이메일 인증과 비밀번호 재설정 안내

## 8. MSA 서비스 통합 아키텍처

### 8.1. 동기 MSA 연동: Property → Member 권한 조회

Property 서비스는 매물 등록·수정 전에 Member 서비스의 내부 권한 API를 동기 호출한다.

```text
Property Service
      │ GET /internal/members/{memberId}/permissions?action=...
      │ X-Trace-Id
      ▼
Member Service
      │ 회원·제재·중개사 상태 조회
      ▼
GlobalResponseDTO<MemberPermissionResponse>
```

- 호출 주체: Property 백엔드
- 호출 방식: Member 응답을 기다리는 Spring `RestClient`
- 접속 주소: `MEMBER_SERVICE_BASE_URL`, 기본값 `http://localhost:8081`
- 책임 경계: Member는 회원 행위 허용 여부를 판단하는 기준 서비스이고, Property는 매물 데이터와 매물 업무를 소유한다. Property의 로컬 회원 상태는 추가 차단 검증에만 사용하며, Member의 거부 판단을 허용으로 바꾸지 않는다.
- 연결·읽기 타임아웃: 각각 기본 `800ms`
- 장애 처리: Member의 HTTP 오류·응답 형식 오류·통신 오류는 Property에서 `MEMBER_API_UNAVAILABLE`로 변환한다. 권한을 확인할 수 없으면 매물 등록·수정을 허용하지 않는다.

허용·거부 조건과 응답 사유는 [API 명세의 매물 쓰기 권한 조회](04-api-specification.md#매물-쓰기-권한-조회)를 기준으로 한다.

### 8.2. 비동기 MSA 연동과 상태 수명주기 — V3 예정

V3 예정 기능은 회원 상태의 수명주기와 서비스 간 후속 처리 정책을 보완한다. Property에는 Member 상태 이벤트 수신과 `eventId` 기반 중복 처리 방지 기반이 있으나, Kafka 리스너는 기본 비활성화 상태다. Member 상태 변경 이벤트의 발행 계약과 운영 활성화 기준은 V3에서 확정한다.

| 기능 영역 | V3 예정 | 서비스별 역할 |
| --- | --- | --- |
| 회원 탈퇴 수명주기 | • 탈퇴 접수 및 상태 전환<br>• 인증 정보 무효화<br>• 개인정보 처리 절차 구성 | • Member: 탈퇴와 보안 처리<br>• Property: 기존 공개 매물·관심 매물·알림 처리 정책 적용 |
| 회원 제재 수명주기 | • 제재 등록 및 수동 해제<br>• 종료 시각이 지난 제재 처리<br>• 다른 활성 제재를 고려한 계정 상태 복원 | • Member: 제재 상태와 계정 상태 결정<br>• Property: 제재 범위와 해제 결과 반영 |
| 중개소 운영 상태 | • 외부 기관 정보 주기적 확인<br>• 조회 실패 재시도<br>• 정상·휴업·폐업 상태 전환 관리 | • Member: 중개소 상태 확인 및 전환<br>• Property: 쓰기 제한, 기존 매물 처리와 영업 재개 정책 적용 |
| 서비스 간 후속 처리 | • 상태 변경이 매물·관심 매물·알림에 미치는 범위 확정<br>• 처리 실패 및 재처리 기준 정리 | • Member: 기준 상태와 변경 정보 제공<br>• Property: 서비스별 후속 처리 수행 |
| 연동 운영 기준 | • 내부 API 보안 및 인증 정책<br>• 타임아웃과 장애 시 차단 원칙<br>• 상태 변경 전달 운영 기준 | • Member·Property·Gateway: 내부 경계와 인증 정책을 일관되게 적용 |

#### 8.2.1. `<Kafka>` 상태 변경 전달(부가 연동)

Kafka는 즉시 응답이 필요하지 않은 상태 변경 전파와 후속 처리에 선택적으로 적용하는 것을 권장한다.

```text
Member 상태 변경
    ↓ 이벤트 발행
Kafka Topic 저장·전달
    ↓ 이벤트 수신
Property 로컬 상태 반영
    ↓
매물·관심 매물·알림 후속 처리
```

Kafka는 이벤트를 저장·전달하고, 실제 업무 처리는 이벤트를 받은 Property가 수행한다.

| 상태 변경 | Property 서비스 |
| --- | --- |
| 회원 탈퇴 | 로컬 탈퇴 상태 갱신 → 공개 매물 숨김 → 관심 매물·알림 처리 |
| 제재 등록·해제·기간 종료 | 로컬 제재 상태 갱신 → 쓰기 제한 적용·해제 |
| 중개사 승인·비활성 | 로컬 중개사 상태 갱신 → 중개사 권한 반영 |
| 중개소 휴업·폐업·영업 재개 | 로컬 영업 상태 갱신 → 쓰기 제한·기존 매물·영업 재개 처리 |

- **MSA 역할 분리**: 요청 시점에 바로 결정해야 하는 권한 판정은 Member 내부 API를 사용하고, Kafka는 상태 변경을 전달하는 부가 연동으로 사용
- **적용 방향**: 서비스 간 연동을 검증한 후 필요한 기능부터 단계적으로 활성화
