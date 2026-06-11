package erp.backEnd.repository;

import erp.backEnd.dto.po.StockUsageResponse;
import erp.backEnd.dto.po.StockUsageSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface StockUsageRepositoryCustom {
    Page<StockUsageResponse> searchPage(StockUsageSearchCondition condition, Pageable pageable);

    Optional<StockUsageResponse> findDetail(Long id);
}
