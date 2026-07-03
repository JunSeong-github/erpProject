package erp.backEnd.service;

import erp.backEnd.dto.po.StockUsageCreateRequest;
import erp.backEnd.dto.po.StockUsageResponse;
import erp.backEnd.dto.po.StockUsageSearchCondition;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.StockUsage;
import erp.backEnd.exception.BusinessException;
import erp.backEnd.exception.ErrorCode;
import erp.backEnd.repository.ItemRepository;
import erp.backEnd.repository.StockUsageRepository;
import erp.backEnd.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockUsageServiceImpl implements StockUsageService {

    private final StockUsageRepository stockUsageRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public StockUsage create(StockUsageCreateRequest req) {
        if (req.getUsageQty() == null || req.getUsageQty() <= 0) {
            throw new IllegalArgumentException("사용량은 1 이상이어야 합니다.");
        }

        Item item = itemRepository.findById(req.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("품목 없음: " + req.getItemId()));

        StockUsage usage = StockUsage.create(
                item,
                req.getPurpose(),
                req.getUsagePlace(),
                req.getUsageQty(),
                req.getUsageDate(),
                req.getRemark()
        );

        return stockUsageRepository.save(usage);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockUsageResponse> findSearchPage(StockUsageSearchCondition condition, Pageable pageable) {
        return stockUsageRepository.searchPage(condition, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public StockUsageResponse getDetail(Long id) {
        return stockUsageRepository.findDetail(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.STOCK_CACHE, allEntries = true)  // 사용 승인으로 재고 차감 → 재고 캐시 무효화
    public void approve(Long id) {
        StockUsage usage = stockUsageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("재고 사용 내역 없음: " + id));

        // 요청 상태만 승인 가능(메모리 상태 전이). 여기서 검증에 실패하면 재고를 건드리지 않는다.
        usage.approve();

        // 승인 시 실재고를 원자적으로 차감한다.
        // UPDATE ... WHERE stock_qty >= qty 로 "읽고-검사-차감"이 DB 단일 문장에서 원자적으로 처리되므로,
        // 동일 품목을 동시에 승인해도 재고가 음수가 되는 check-then-act 경합이 발생하지 않는다.
        int updated = itemRepository.decreaseStock(usage.getItem().getId(), usage.getUsageQty());
        if (updated == 0) {
            log.warn("재고 부족 승인 차단 - itemId={}, 사용요청={}",
                    usage.getItem().getId(), usage.getUsageQty());
            // 예외 → 트랜잭션 롤백 → 위 usage.approve() 상태전이도 함께 취소됨
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }
    }

    @Override
    @Transactional
    public void reject(Long id, String reason) {
        StockUsage usage = stockUsageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("재고 사용 내역 없음: " + id));

        usage.reject(reason);
    }
}
