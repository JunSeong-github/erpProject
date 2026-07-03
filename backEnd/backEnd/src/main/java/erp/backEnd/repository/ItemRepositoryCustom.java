package erp.backEnd.repository;

import erp.backEnd.dto.po.ItemResponse;
import erp.backEnd.dto.po.ItemSearchCondition;
import erp.backEnd.dto.po.StockResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemRepositoryCustom {
    Page<ItemResponse> searchPageComplex(ItemSearchCondition condition, Pageable pageable);

    Page<StockResponse> searchStockPage(ItemSearchCondition condition, Pageable pageable);

    /**
     * 집계(입고합 - 승인사용합)로 재고를 재계산한다.
     * 정상 조회는 item.stock_qty 컬럼을 정답으로 쓰고, 이 메서드는 컬럼값 검증/대사(reconciliation) 용도로만 사용한다.
     */
    Long getAggregatedStock(Long itemId);
}
