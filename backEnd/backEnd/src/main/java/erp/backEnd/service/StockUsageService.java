package erp.backEnd.service;

import erp.backEnd.dto.po.StockUsageCreateRequest;
import erp.backEnd.dto.po.StockUsageResponse;
import erp.backEnd.dto.po.StockUsageSearchCondition;
import erp.backEnd.entity.StockUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockUsageService {

    StockUsage create(StockUsageCreateRequest request);

    Page<StockUsageResponse> findSearchPage(StockUsageSearchCondition condition, Pageable pageable);

    StockUsageResponse getDetail(Long id);

    void approve(Long id);

    void reject(Long id, String reason);
}
