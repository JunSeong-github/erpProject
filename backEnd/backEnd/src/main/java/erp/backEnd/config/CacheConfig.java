package erp.backEnd.config;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;

/**
 * Spring Cache 추상화 활성화 + Redis 캐시 설정.
 *
 * spring-boot-starter-data-redis 가 클래스패스에 있으면 Spring Boot 가
 * RedisCacheManager 를 자동 구성한다. 아래 RedisCacheConfiguration 빈이
 * 그 매니저의 "기본 캐시 설정"으로 채택되고, Customizer 로 캐시별 설정을 덮어쓴다.
 *
 * - 값 직렬화: JSON (Jackson3 기반 GenericJacksonJsonRedisSerializer)
 *   ※ Spring Boot 4.0 은 Jackson3(tools.jackson) 를 쓰므로 Jackson2 기반
 *     GenericJackson2JsonRedisSerializer 는 사용 불가.
 * - 키 직렬화: 문자열 (redis-cli 에서 키가 그대로 보임)
 *
 * TTL 정책 (캐시별로 다름):
 * - items(품목 목록): 변경이 드물어 10분. @CacheEvict 로 즉시 무효화 + 안전장치.
 * - stock(재고 현황): 실시간성이 중요 → 30초로 짧게. 입고/사용승인 때마다 evict 하지만,
 *   혹시 무효화 경로가 누락돼도 최대 30초 뒤에는 최신값으로 갱신되도록 상한을 짧게 둔다.
 */
@Configuration
@EnableCaching
// prod(운영, Redis 없음)와 test(CI, 외부 Redis 없음) 를 제외한 환경(local/default)에서만 캐싱 활성화.
// 이 두 프로필에서는 이 설정이 빠져 @EnableCaching 자체가 없으므로 RedisCacheManager 를 요구하지 않는다.
@Profile("!prod & !test")
public class CacheConfig {

    /**
     * 캐시 이름 상수 (오타 방지용, 서비스의 @Cacheable/@CacheEvict 와 공유).
     * static final 이라 컴파일 상수 → prod 에서 이 빈이 안 만들어져도 서비스 코드에서 그대로 참조 가능.
     */
    public static final String ITEMS_CACHE = "items";
    public static final String STOCK_CACHE = "stock";

    /** 직렬화/‑null 정책 공통 베이스 (TTL 은 호출부에서 지정) */
    private RedisCacheConfiguration baseConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer()));
    }

    /**
     * 캐시 값 JSON 직렬화기.
     *
     * Jackson3 GenericJacksonJsonRedisSerializer 는 기본적으로 타입정보(@class)를
     * 넣지 않아, 역직렬화 시 구체 타입(DTO)이 아닌 LinkedHashMap 으로 복원된다.
     * → CachedPage 등 객체 캐시가 ClassCastException 을 일으킴.
     *
     * 그래서 default typing 을 켜서 @class 를 기록하되, 역직렬화 가젯 공격을 막기 위해
     * 우리 DTO 패키지와 표준 라이브러리 타입만 허용하는 검증기를 건다.
     */
    private GenericJacksonJsonRedisSerializer jsonSerializer() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("erp.backEnd.")   // 우리 DTO
                .allowIfSubType("java.util.")      // ArrayList 등 컬렉션
                .allowIfSubType("java.lang.")      // String, Long 등
                .allowIfSubType("java.math.")      // BigDecimal
                .build();
        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(ptv)
                .build();
    }

    /** 기본 캐시 설정(TTL 10분) — 별도 지정 없는 캐시(items 포함)에 적용 */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        return baseConfig().entryTtl(Duration.ofMinutes(10));
    }

    /** 캐시별 개별 설정: 재고(stock)는 TTL 30초 */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder.withCacheConfiguration(
                STOCK_CACHE, baseConfig().entryTtl(Duration.ofSeconds(30)));
    }
}
