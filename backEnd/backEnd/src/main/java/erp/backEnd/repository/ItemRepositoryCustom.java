package erp.backEnd.repository;

import erp.backEnd.dto.po.ItemResponse;
import erp.backEnd.dto.po.ItemSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemRepositoryCustom {
    Page<ItemResponse> searchPageComplex(ItemSearchCondition condition, Pageable pageable);
}
