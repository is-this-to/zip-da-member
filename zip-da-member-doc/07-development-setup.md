# 개발 환경 세팅

[README로 돌아가기](../README.md)

## 1. 필수 도구

| 도구 | 기준 |
| --- | --- |
| JDK | 21 |
| MySQL | CI 기준 8.4 |
| Gradle | 저장소의 Wrapper 사용 |
| 선택 의존성 | MinIO, Google Cloud 자격 증명, 외부 API 접근 |

Java 21이 설치되어 있고, 저장소에 포함된 Gradle Wrapper가 정상 실행되는지 확인한다.

```bash
java -version
./gradlew --version
```

## 2. 환경 변수

### 2.1. 로컬 개발 환경

- 로컬 실행에 필요한 환경 변수는 `.env` 파일에 설정한다.
- `.env` 파일을 설정하고, Spring Boot 애플리케이션 또는 Gradle `bootRun`을 실행할 때 환경 변수가 적용된다.
  - IntelliJ IDEA 실행 설정 예시: Run/Debug Configuration 환경 변수 파일(`.env`)의 경로 지정
  - Git Bash 명령에서 경로 지정 예시: `source` 명령으로 환경 변수 파일을 불러와 설정

  ```bash
  set -a
  source /path/to/.env
  set +a
  ./gradlew bootRun
  ```

- 실제 비밀값은 로컬 `.env`에만 보관하고 문서·Git·로그에는 기록하지 않는다.

```dotenv
APP_PORT=8081
APP_DESCRIPTION="ZIPDA Member local"
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=zipda_member
DB_USER=zipda_local
DB_PASSWORD=[REDACTED_SECRET]
JWT_SECRET=[REDACTED_SECRET_BASE64]
GATEWAY_URI=http://localhost:8080
FRONTEND_CALLBACK_URI=http://localhost:5173/oauth2/callback
MAIL_USER=local@example.invalid
MAIL_PASSWORD=[REDACTED_SECRET]
KAKAO_CLIENT_ID=[REDACTED_SECRET]
KAKAO_CLIENT_SECRET=[REDACTED_SECRET]
MINIO_ENDPOINT=http://localhost:9000
MINIO_BUCKET=zipda-local
MINIO_ACCESS_KEY=[REDACTED_SECRET]
MINIO_SECRET_KEY=[REDACTED_SECRET]
MINIO_PROFILE_PATH=profiles
MINIO_DOCUMENT_PATH=agent-documents
MOLIT_URI=https://example.invalid
MOLIT_API_KEY=[REDACTED_SECRET]
MOLIT_SERVICE_DOMAIN=local
NTS_URI=https://example.invalid
NTS_API_KEY=[REDACTED_SECRET]
PASSWORD_RESET_FRONTEND_URI=http://localhost:5173/password-reset
PASSWORD_RESET_TOKEN_EXPIRY_MINUTES=30
ZIPDA_OCR_ENABLED=false
GOOGLE_CLOUD_PROJECT=local-project
GOOGLE_CLOUD_VISION_INPUT_BUCKET=local-input
GOOGLE_CLOUD_VISION_OUTPUT_BUCKET=local-output
GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/service-account.json
ZIPDA_OCR_TIMEOUT_SECONDS=30
ZIPDA_OCR_MAX_FILE_SIZE_BYTES=10485760
ZIPDA_OCR_MAX_PDF_PAGES=10
```

- `JWT_SECRET`은 JJWT HMAC 요구 길이를 만족하는 Base64/Base64URL 값이어야 한다.

- `application.yaml`은 기본 설정을 읽고, `${변수명}` 형식으로 주입된 환경 변수를 참조한다.

### 2.2. 운영 환경 참고

- 운영에서는 중요정보를 Kubernetes `Secret`, 일반 설정을 `ConfigMap`으로 관리하고, `Deployment`가 이를 환경 변수로 주입한다.
- 운영 `Deployment`에서 `SPRING_PROFILES_ACTIVE=prod`를 설정하면 `application-prod.yaml` 설정이 적용된다.

```text
Kubernetes Secret / ConfigMap
    → Deployment 환경 변수 주입
    → application.yaml의 ${변수명} 참조
    → Spring Boot 설정 적용
```

## 3. DB 생성 및 스키마 적용

- `spring.jpa.hibernate.ddl-auto=none`이므로 애플리케이션이 DB나 테이블을 자동 생성하지 않는다. 
- 다음 순서로 DB를 생성한 뒤 팀에서 제공하는 DB 구조 정의 SQL(schema SQL)을 실행해야 한다.

### 3.1. DB 생성

MySQL 8.4 기준 예시이다. DB 이름은 `DB_NAME`과 동일하게 맞춘다.

```sql
CREATE DATABASE IF NOT EXISTS zipda_member
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

### 3.2. 스키마 SQL 실행

DB 구조 정의(schema) 파일은 사용자가 다시 제공할 기준 SQL을 사용한다. 파일을 받기 전에는 Entity를 기준으로 임의의 테이블 생성 SQL(DDL)을 생성하거나 적용하지 않는다.

```powershell
# 실제 제공받은 절대 경로로 변경
$schemaPath = 'E:/path/to/zipda-member-schema.sql'

mysql `
  --host=127.0.0.1 `
  --port=3306 `
  --user=root `
  --password `
  --database=zipda_member `
  --execute="source $schemaPath"
```

## 4. Swagger/OpenAPI 설정

### 4.1. 로컬 개발 환경

- Member 서비스는 OpenAPI JSON을 `http://localhost:8081/api-docs`에서 제공한다.
- 자체 Swagger UI는 기본적으로 비활성화되어 있다.
- Member만 실행하면서 Swagger UI가 필요하면 공통 설정을 변경하지 않고 로컬 profile에서만 활성화한다.

```yaml
# src/main/resources/application-local.yaml 예시
springdoc:
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
  api-docs:
    path: /api-docs
```

환경 변수를 적용한 Git Bash에서 로컬 profile로 실행한다.

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

- 로컬 Swagger UI: `http://localhost:8081/swagger-ui.html`
- 보호 API의 인증·권한 흐름은 Gateway를 거쳐 별도로 확인한다.

### 4.2. 운영 환경

- Member 서비스의 자체 Swagger UI는 비활성화하고 `/api-docs`만 제공한다.
- Gateway(SCG 서비스)가 Member와 Property 서비스의 OpenAPI JSON을 모아 통합 Swagger UI를 제공한다.

```yaml
# src/main/resources/application-prod.yaml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    path: /api-docs
```

```text
https://zipda-be.meerkat.p-e.kr/docs
    ├─ /member-service/api-docs   → Member /api-docs
    └─ /property-service/api-docs → Property /api-docs
```

## 5. 검증

```powershell
# 단위/통합 테스트 소스가 있을 때 실행
.\gradlew.bat test

# 실행 가능한 JAR 생성
.\gradlew.bat clean bootJar
```

정상 기준은 다음과 같다.

- 테스트: 실패한 테스트 없이 `BUILD SUCCESSFUL`이 표시된다.
- 테스트 결과: `build/reports/tests/test/index.html`에서 실행 결과를 확인할 수 있다.
- JAR 생성: `BUILD SUCCESSFUL`이 표시되고 `build/libs`에 실행 가능한 JAR 파일이 생성된다.
