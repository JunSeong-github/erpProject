package erp.backEnd.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * springdoc-openapi(Swagger) 기본 문서 정보 + 세션 쿠키 인증 + 태그 표시 순서 설정.
 * - Swagger UI: /swagger-ui.html  (실제 리소스는 /swagger-ui/index.html 로 리다이렉트)
 * - OpenAPI 스펙(JSON): /v3/api-docs
 *
 * [세션 쿠키 인증 테스트 방법]
 * 이 서버는 세션 기반 로그인이라 로그인 성공 시 JSESSIONID 쿠키가 발급된다.
 * Swagger UI 는 백엔드와 같은 오리진(localhost:8080)에서 제공되므로, 브라우저가
 * 이후 "Try it out" 요청에 JSESSIONID 쿠키를 자동으로 실어 보낸다.
 *   1) POST /auth/login 을 Try it out 으로 호출해 로그인 (Set-Cookie: JSESSIONID)
 *   2) 이후 인증이 필요한 API(/notifications/** 등)를 그대로 Try it out 하면 통과
 * 아래 cookieAuth 스킴은 "이 API 는 세션 인증이 필요하다"는 표시(자물쇠 아이콘)를 위한 문서화다.
 *
 * [프로파일] @Profile("!prod") : 운영(prod)에서는 이 설정 빈들이 생성되지 않는다.
 *  실제 엔드포인트 차단은 application.yml 의 prod 프로필에서 springdoc.api-docs/swagger-ui.enabled=false 로 처리.
 */
@Configuration
@Profile("!prod")
public class OpenApiConfig {

    private static final String COOKIE_AUTH = "cookieAuth";

    /**
     * 태그(그룹) 표시 순서. 컨트롤러 @Tag 이름과 정확히 일치해야 한다.
     * 여기에 없는 태그는 목록 맨 뒤로 밀린다.
     */
    private static final List<String> TAG_ORDER = List.of(
            "인증(Auth)",
            "발주(PO)",
            "입고(Receipt)",
            "재고 사용(출고)",
            "품목(Item)",
            "공급사(Vendor)",
            "멤버(Member)",
            "알림(Notification)",
            "시스템(System)"
    );

    @Bean
    public OpenAPI erpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ERP API")
                        .description("ERP 백엔드 API 문서 (발주/재고/알림 등). "
                                + "세션 로그인: POST /auth/login 호출 후 같은 브라우저 세션으로 인증이 유지됩니다.")
                        .version("v0.0.1"))
                // JSESSIONID 쿠키 기반 인증 스킴 등록 → Swagger UI 에 자물쇠/Authorize 노출
                .components(new Components()
                        .addSecuritySchemes(COOKIE_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("세션 로그인 시 발급되는 세션 쿠키. "
                                        + "같은 오리진이라 로그인 후 브라우저가 자동으로 전송한다.")))
                // 전역 요구사항으로 등록(대부분 API 가 인증 대상). 자물쇠 아이콘 표시용.
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH));
        // ※ 태그 순서는 여기서 .tags(...) 로 넣지 않는다.
        //   그러면 컨트롤러 @Tag 로 수집된 태그와 합쳐져 '중복'이 생기고 순서가 꼬인다.
        //   대신 아래 OpenApiCustomizer 에서 springdoc 이 태그를 다 모은 뒤 정렬/중복제거한다.
    }

    /**
     * springdoc 이 컨트롤러 @Tag 를 모두 수집한 '뒤'에 실행되어,
     * 태그 목록을 이름 기준으로 중복 제거하고 TAG_ORDER 순서로 재정렬한다.
     * Swagger UI 는 이 tags 배열 순서대로 그룹을 렌더링하므로 인증이 맨 위로 온다.
     */
    @Bean
    public OpenApiCustomizer sortTagsCustomizer() {
        return openApi -> {
            if (openApi.getTags() == null || openApi.getTags().isEmpty()) {
                return;
            }
            // 이름 기준 중복 제거(먼저 등장한 것 유지)
            Map<String, Tag> byName = new LinkedHashMap<>();
            for (Tag tag : openApi.getTags()) {
                byName.putIfAbsent(tag.getName(), tag);
            }
            // TAG_ORDER 순서로 정렬, 목록에 없는 태그는 뒤로
            List<Tag> sorted = new ArrayList<>(byName.values());
            sorted.sort(Comparator.comparingInt(tag -> {
                int idx = TAG_ORDER.indexOf(tag.getName());
                return idx < 0 ? Integer.MAX_VALUE : idx;
            }));
            openApi.setTags(sorted);
        };
    }
}
