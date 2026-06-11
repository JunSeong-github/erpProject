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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public void approve(Long id) {
        StockUsage usage = stockUsageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("재고 사용 내역 없음: " + id));

        // 승인 시 실재고 차감 → 현재 가용재고보다 많이 사용할 수 없음
        Long currentStock = itemRepository.getCurrentStock(usage.getItem().getId());
        if (usage.getUsageQty() > currentStock) {
            log.warn("재고 부족 승인 차단 - itemId={}, 현재고={}, 사용요청={}",
                    usage.getItem().getId(), currentStock, usage.getUsageQty());
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }

        usage.approve();
    }

    @Override
    @Transactional
    public void reject(Long id, String reason) {
        StockUsage usage = stockUsageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("재고 사용 내역 없음: " + id));

        usage.reject(reason);
    }
}
