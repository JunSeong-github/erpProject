package erp.backEnd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * [0단계] JPA Auditing 의 @CreatedBy / @LastModifiedBy 를 채우는 AuditorAware.
 *
 * <p>지금까지는 이 빈이 없어 created_by 가 항상 NULL 이었다(ReceiptBulkRepository 주석 참고).
 * 이제 로그인 세션의 loginId 를 저장하므로, "발주/재고사용의 작성자 = 신청자" 가 실제 DB 에 남고
 * 승인/반려 알림의 수신 대상(신청자)을 createdBy 로 식별할 수 있다.</p>
 *
 * <p>@EnableJpaAuditing 은 타입이 AuditorAware 인 단일 빈을 자동으로 사용한다.</p>
 */
@Configuration
public class AuditorAwareConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.empty(); // 비로그인 요청은 created_by 를 채우지 않는다
            }
            return Optional.of(auth.getName());
        };
    }
}
