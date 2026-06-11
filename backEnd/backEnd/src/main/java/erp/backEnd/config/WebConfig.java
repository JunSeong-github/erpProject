package erp.backEnd.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // CORS 설정은 SecurityConfig 의 CorsConfigurationSource 로 일원화되었습니다.
    // (Spring Security 필터 단계에서 처리 → 중복 헤더 방지)

}
