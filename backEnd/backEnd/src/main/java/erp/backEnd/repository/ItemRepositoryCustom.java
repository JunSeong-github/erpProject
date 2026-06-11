package erp.backEnd.repository;

import erp.backEnd.dto.po.ItemResponse;
import erp.backEnd.dto.po.ItemSearchCondition;
import erp.backEnd.dto.po.StockResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemRepositoryCustom {
    Page<ItemResponse> searchPageComplex(ItemSearchCondition condition, Pageable pageable);

    Page<StockResponse> searchStockPage(ItemSearchCondition condition, Pageable pageable);

    Long getCurrentStock(Long itemId);
}
