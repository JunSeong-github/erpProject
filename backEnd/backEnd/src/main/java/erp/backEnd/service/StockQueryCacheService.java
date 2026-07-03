package erp.backEnd.service;

import erp.backEnd.config.CacheConfig;
import erp.backEnd.dto.po.CachedPage;
import erp.backEnd.dto.po.ItemSearchCondition;
import erp.backEnd.dto.po.StockResponse;
import erp.backEnd.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 현황 조회 캐싱 전용 빈.
 *
 * @Cacheable 은 스프링 AOP 프록시를 거쳐야 동작하는데, 같은 클래스 내부 호출
 * (self-invocation)은 프록시를 안 타므로 캐싱이 무시된다. 그래서 캐싱 대상 메서드를
 * ItemServiceImpl 과 분리된 이 빈에 두고, ItemServiceImpl 이 외부 빈 호출로 사용한다.
 *
 * 캐시 키는 (검색조건 + 페이지번호 + 페이지크기) 조합. 조건/페이지마다 다른 결과가
 * 다른 키로 저장된다. (정렬은 재고 쿼리에서 itemName asc 로 고정이라 키에 넣지 않음)
 */
@Service
@RequiredArgsConstructor
public class StockQueryCacheService {

    private final ItemRepository itemRepository;

    @org.springframework.cache.annotation.Cacheable(
            cacheNames = CacheConfig.STOCK_CACHE,
            key = "(#condition.itemName ?: '') + '|' + (#condition.itemCode ?: '') + '|' + #pageable.pageNumber + '|' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public CachedPage<StockResponse> getStockPage(ItemSearchCondition condition, Pageable pageable) {
        return CachedPage.of(itemRepository.searchStockPage(condition, pageable));
    }
}
