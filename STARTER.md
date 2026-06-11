# STARTER.md — 동일 스택 새 프로젝트 세팅 가이드

이 문서는 `erpProject`와 **같은 기술 스택·구조**로 새 프로젝트를 시작할 때 사용하는 체크리스트입니다.
새 프로젝트 첫 메시지에 이 파일을 던지고 아래 "킥오프 템플릿"만 채워 주면 동일하게 세팅됩니다.

---

## 1. 킥오프 메시지 템플릿 (복사해서 채우기)

```
STARTER.md 기준으로 새 프로젝트 세팅해줘. 기존 erpProject 규칙 그대로 적용.
- 프로젝트명:        (예: ticketSystem)
- 위치:             (예: C:\ticketProject)
- DB:               MySQL / 스키마명 (예: ticket) / 계정 / 비번
- 권한(Role):       (예: ADMIN 관리자 / EMPLOYEE 직원 / GUEST 게스트)
- 로그인:           세션 + BCrypt + 시드계정
- 첫 도메인:        (예: ticket — 제목/내용/상태/담당자 …)
- 깃 리포:          새로 만들지 여부
```

> 실제로 정해야 하는 건 **DB 접속정보 / Role 종류 / 첫 도메인 1개**뿐. 나머지는 아래 스택·구조를 그대로 복제.

---

## 2. 기술 스택

### 백엔드
- **Spring Boot 4.0 / Java 17 / Gradle**
- 의존성
  - `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security`
  - QueryDSL `5.0.0:jakarta` (+ APT: querydsl-apt, jakarta.annotation-api, jakarta.persistence-api)
  - MySQL `mysql-connector-j` + p6spy(`p6spy-spring-boot-starter`) 쿼리 로깅
  - Lombok
- 설정: JPA Auditing(`@EnableJpaAuditing`), `ddl-auto: update`, `local` 프로필, 세션 로그인 + BCrypt

### 프론트엔드
- **React 19 / Vite 7 / TypeScript**
- `react-router-dom` 7 (`createBrowserRouter`, `basename`)
- `@tanstack/react-query` 5
- `axios` (`withCredentials: true` — 세션 쿠키)
- 배포: `gh-pages` (GitHub Pages)

---

## 3. 디렉터리 구조 (모노레포)

```
<projectRoot>/
├─ backEnd/
│  ├─ backEnd/            ← Spring Boot (gradle)
│  │  └─ src/main/java/<group>/<app>/
│  │     ├─ controller/        REST 엔드포인트
│  │     ├─ service/           인터페이스 + Impl
│  │     ├─ repository/        JpaRepository + *Custom + *Impl(QueryDSL)
│  │     ├─ entity/            BaseEntity / BaseTimeEntity 상속
│  │     ├─ dto/               요청/응답 DTO (@QueryProjection 활용)
│  │     ├─ enumeration/       상태/권한 enum (code+label)
│  │     ├─ exception/         BusinessException / ErrorCode / GlobalExceptionHandler / ErrorResponseDto
│  │     ├─ config/            SecurityConfig / WebConfig / DataInitializer
│  │     └─ security/          CustomUserDetailsService
│  └─ frontEnd/           ← React (vite)
│     └─ src/
│        ├─ app/          router.tsx / AppLayout / AuthContext / RequireAuth / queryClient
│        ├─ auth/         LoginPage
│        ├─ components/   AppHeader
│        ├─ <domain>/     <Domain>ListPage / <Domain>CreatePage / <Domain>DetailPage
│        └─ lib/axios.ts
└─ (git 루트는 projectRoot)
```

---

## 4. 그대로 복제하는 공통 패턴 (직접 안 정해도 됨)

**백엔드**
- `BaseTimeEntity`(createdDate/lastModifiedDate) → `BaseEntity`(createdBy/lastModifiedBy) 상속 구조
- 상태/권한 enum은 `code` + `label`(한글) + `fromCode()` 패턴
- 목록 조회: `*RepositoryCustom` + `*RepositoryImpl`에서 QueryDSL `searchPage(condition, pageable)` → `Page<Response>`
- 예외: 도메인 오류는 `throw new BusinessException(ErrorCode.XXX)` →
  `GlobalExceptionHandler`가 `ErrorResponseDto`(code/message) 본문으로 응답
  - ⚠️ **한글 메시지는 HTTP 헤더로 못 내려감(Tomcat 제약)**. 한글 사용자 메시지는 반드시 `ErrorCode` + `BusinessException`으로 **응답 본문**에 담을 것
- 보안: `SecurityConfig`에서 CORS 일원화(`CorsConfigurationSource`), 세션 + BCrypt,
  로그인 식별만 담당하고 기능별 접근은 `authorizeHttpRequests`에서 경로별 `hasRole(...)`로 잠금
- 시드 계정: `DataInitializer`(ApplicationRunner)에서 `existsByLoginId` 체크 후 BCrypt 저장

**프론트엔드**
- `AuthProvider`가 앱 로드 시 `/auth/me`로 세션 확인 → `RequireAuth` 가드로 미로그인 시 `/login` 리다이렉트
- 목록 페이지 골격: 검색조건 state + URL 쿼리파라미터 동기화 + 페이징 + `keepPreviousData`
- API 호출은 `src/lib/axios.ts`의 `api` 인스턴스(`withCredentials: true`) 사용
- 권한별 UI: `useAuth().user.role`로 버튼 노출 제어(+ 백엔드 403 이중 방어)

---

## 5. 세팅 순서 (내가 진행하는 절차)

1. 디렉터리/모노레포 생성, 깃 초기화
2. 백엔드: `build.gradle`(의존성), `application.yml`(DB/프로필/ddl-auto), 메인 클래스(`@EnableJpaAuditing`)
3. 공통 기반: `BaseTimeEntity`/`BaseEntity`, `exception`(ErrorCode/BusinessException/GlobalExceptionHandler/ErrorResponseDto)
4. 보안: `SecurityConfig`(CORS+세션+BCrypt), `Role` enum, `Member`(loginId/password/role), `CustomUserDetailsService`, `AuthController`(login/me/logout), `DataInitializer` 시드
5. 첫 도메인: entity → repository(+QueryDSL) → service → controller → DTO
6. 프론트: vite 프로젝트, `axios`, `AuthContext`/`RequireAuth`/`router`/`AppLayout`/`AppHeader`, `LoginPage`
7. 첫 도메인 화면: List/Create/Detail
8. **빌드 검증**: `./gradlew compileJava` + `npm run build`
9. 시드 계정으로 로그인 E2E 확인

---

## 6. 적용할 작업 규칙 (erpProject와 동일)

- 질문/선택지는 **한글**로
- 커밋 메시지에 **Co-Authored-By 트레일러 금지**
- 기능 작업 후 **항상 빌드/컴파일 오류 검사**
- 커밋/푸시는 **요청 시에만**
