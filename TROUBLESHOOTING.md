# 트러블슈팅 (주요 사례)

배포·운영 중 실제로 부딪힌 문제와 해결 과정을 **문제 → 원인 → 해결** 순서로 정리했습니다.

---

## 1. SPA + 세션 인증의 Cross-Site 쿠키 문제

### 문제
배포 후 프론트(`github.io`)와 백엔드(`onrender.com`)의 출처(origin)가 달라지면서, 로그인 요청 자체는 성공하지만 이후 요청에서 세션이 유지되지 않아 승인·반려 등 인증이 필요한 API에서 **401 Unauthorized**가 발생했습니다.

### 원인
브라우저는 서로 다른 사이트로 나가는 cross-site 요청에는 기본값인 `SameSite=Lax` 쿠키를 전송하지 않습니다. 즉 로그인 시 발급된 세션 쿠키(`JSESSIONID`)가 이후 요청에 실려 나가지 않아 서버가 매 요청을 비인증으로 처리한 것입니다.

### 해결
- **쿠키 속성 변경**: 세션 쿠키를 `SameSite=None; Secure`로 설정해 HTTPS 기반 cross-site 요청에도 쿠키가 전송되도록 했습니다.
- **자격증명 허용 일치**: 서버는 CORS `allowCredentials=true`, 프론트는 axios `withCredentials=true`로 양쪽을 맞췄습니다.
- **CORS 일원화**: CORS 설정이 여러 곳에 흩어져 `Access-Control-Allow-Origin` 헤더가 중복되던 문제를, `SecurityConfig` 한 곳으로 모아 헤더 중복을 제거했습니다.

---

## 2. 배포 DB 전환 (MySQL → PostgreSQL) & 한글 에러 메시지 인코딩

### DB 전환
Render 무료 티어에서 제공하는 DB가 PostgreSQL이라 로컬 개발 환경(MySQL)과 분리해야 했습니다.

- **원인**: DB 벤더가 다르면 드라이버, 배치 INSERT 파라미터, 접속 정보가 모두 달라집니다.
- **해결**: `local`/`prod` 스프링 프로필로 드라이버·배치 파라미터·접속정보를 분기 처리해, 코드 변경 없이 프로필만으로 로컬(MySQL)과 운영(PostgreSQL)을 전환하도록 했습니다.

### 한글 에러 메시지 인코딩
- **문제**: 한글 에러 메시지를 HTTP 응답 헤더에 담았더니 톰캣 단계에서 비-ASCII 문자가 누락되어 메시지가 깨졌습니다.
- **원인**: HTTP 헤더는 기본적으로 ASCII 기반이라 비-ASCII 값이 안전하게 전달되지 않습니다.
- **해결**: `ErrorCode` + `BusinessException`을 도입해 에러 메시지를 응답 **본문(JSON)** 에 담아 일관되게 처리했습니다.

---

## 3. Redis 캐싱 self-invocation 프록시 함정

### 문제
조회가 잦은 메서드에 `@Cacheable`을 붙였는데도 캐시가 전혀 동작하지 않고 매번 실제 쿼리가 실행됐습니다.

### 원인
Spring Cache는 AOP 프록시로 동작합니다. 같은 빈 내부에서 자기 자신의 메서드를 호출(self-invocation)하면 프록시를 거치지 않고 원본 메서드로 직접 진입하기 때문에, `@Cacheable`·`@CacheEvict` 같은 캐시 어드바이스가 적용되지 않습니다.

### 해결
캐싱 대상 메서드를 **별도 빈으로 분리**하고, 외부에서 그 빈을 주입받아 호출하도록 리팩터링해 항상 프록시를 경유하도록 만들었습니다.
