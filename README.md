# ERP 발주·재고 관리 시스템

발주부터 입고, 재고, 재고 사용까지의 흐름을 다루는 사내 ERP 형태의 웹 애플리케이션입니다.
실무에서 쓰는 구조를 직접 설계해보고 싶어서 Spring Boot + React로 만들었습니다.

<!-- 배포 링크가 있다면 여기에: 🔗 데모 https://... / 데모 계정 admin / admin1234 -->
<!-- 메인 화면, 발주 목록, 재고 현황 등 스크린샷/GIF를 2~3장 넣으면 좋습니다 -->

## 한눈에 보기

- 기간 / 인원: 개인 프로젝트
- 담당: 백엔드 · 프론트엔드 전체
- 핵심: 발주 결재(승인/반려) → 입고 → 재고 → 재고 사용 결재까지 이어지는 업무 흐름과 권한 분리

## 사용 기술

**Backend** &nbsp; Java 17, Spring Boot 4, Spring Data JPA, QueryDSL, Spring Security, MySQL
**Frontend** &nbsp; React 19, TypeScript, Vite, React Router, TanStack Query, Axios

기술 선택 이유를 짧게 적으면:

- **QueryDSL** — 검색 조건이 여러 개인 목록 화면이 많아서, 동적 쿼리를 타입 안전하게 짜려고 도입했습니다.
- **TanStack Query** — 목록/상세 데이터 캐싱과 페이지 이동 시 깜빡임 제거(`keepPreviousData`)를 위해 사용했습니다.
- **Spring Security 세션** — 토큰 만료/리프레시 관리 부담 없이 로그인·권한 분리를 먼저 정확히 구현하는 데 집중하려고 세션 방식을 택했습니다.

## 주요 기능

| 영역 | 내용 |
| --- | --- |
| 로그인 / 권한 | 세션 기반 로그인, 관리자/직원 권한 분리, 비로그인 시 로그인 페이지로 가드 |
| 품목 / 공급사 | 등록·수정·삭제, 코드 중복 체크, 조건 검색 + 페이징 |
| 발주 | 작성·수정·삭제, 승인/반려(관리자), 입고 진행 전환 |
| 입고 | 입고 등록 및 누적 현황 관리, 입고 전표 Excel / PDF 다운로드 |
| 재고 | 품목별 현재 재고 조회 (입고 누적 − 승인된 사용량) |
| 재고 사용 | 사용 신청(용도/사용처/수량) → 관리자 승인 시 실제 재고 차감, 반려 처리 |

권한은 화면에서 버튼을 숨기는 것뿐 아니라 서버에서도 막아서, 직원 계정이 직접 승인 API를 호출해도 차단됩니다.

## 시스템 구조

```
React (Vite) ── Axios(withCredentials) ──► Spring Boot REST API ──► MySQL
                     세션 쿠키(JSESSIONID)        Spring Security / JPA·QueryDSL
```

폴더 구조는 책임별로 나눴습니다.

```
backEnd/
 ├─ backEnd/   Spring Boot
 │   controller · service(interface+impl) · repository(JPA+QueryDSL)
 │   entity · dto · enumeration · exception · config · security
 └─ frontEnd/  React
     app(router·인증컨텍스트·가드) · auth · components · <도메인>/페이지 · lib
```

<!-- ERD 이미지를 넣으면 가장 좋습니다: ![ERD](docs/erd.png) -->

## 구현하면서 고민한 점

실제로 막혔거나 선택이 필요했던 부분들입니다.

**1. 재고를 어떻게 관리할까 — 테이블 vs 계산**
재고 수량을 별도 컬럼으로 들고 다니면 입고·사용 때마다 갱신해야 하고, 누락되면 값이 틀어집니다.
그래서 재고를 저장하지 않고 `입고 합계 − 승인된 사용량 합계`로 조회 시점에 계산하도록 했습니다.
덕분에 "사용 신청을 승인하는 순간" 별도 처리 없이 재고에 자동 반영되고, 정합성이 깨질 여지가 줄었습니다.

**2. 승인/반려 권한 분리**
관리자만 결재할 수 있어야 해서, Spring Security에서 해당 경로를 `hasRole("ADMIN")`로 잠그고
프론트에서는 권한에 따라 버튼을 숨겼습니다. 서버·클라이언트 양쪽에서 막아 우회 호출도 막았습니다.

**3. 에러 메시지가 화면에 깨져 나오던 문제**
처음엔 예외 메시지를 응답 헤더로 내려줬는데, 한글 메시지가 톰캣 단계에서 누락돼 엉뚱한 문구가 떴습니다.
원인을 확인한 뒤, 사용자에게 보여줄 메시지는 `ErrorCode` + 공통 예외(`BusinessException`)로 응답 본문에 담아
일관되게 내려주도록 정리했습니다.

**4. SPA + 세션 인증의 CORS/쿠키 처리**
프론트(5173)와 API(8080)가 출처가 달라서, Axios `withCredentials`와 서버의 `allowCredentials`를 맞추고
CORS 설정을 SecurityConfig 한 곳으로 모아 헤더 중복 문제를 없앴습니다.

## 실행 방법

사전 준비: JDK 17, Node.js, MySQL. MySQL에 `erp` 스키마를 만들어 둡니다.

DB 접속 정보는 환경변수로 주입합니다(소스에 비밀번호를 두지 않습니다).

```bash
# 백엔드 (http://localhost:8080)
cd backEnd/backEnd
export DB_USERNAME=root
export DB_PASSWORD=본인_비밀번호
./gradlew bootRun
```

```bash
# 프론트엔드 (http://localhost:5173)
cd backEnd/frontEnd
npm install
npm run dev
```

최초 기동 시 데모 계정이 자동 생성됩니다.

| 권한 | 아이디 | 비밀번호 |
| --- | --- | --- |
| 관리자 | admin | admin1234 |
| 직원 | employee | employee1234 |

## 앞으로

- 권한 게스트 등급 추가 및 기능별 접근 제어 확대
- 출퇴근 / 결재선 설정 / 휴가 결재 등 근태·결재 도메인 확장
