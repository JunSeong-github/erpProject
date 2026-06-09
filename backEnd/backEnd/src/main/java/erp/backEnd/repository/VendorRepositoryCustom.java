package erp.backEnd.repository;

import erp.backEnd.dto.po.VendorResponse;
import erp.backEnd.dto.po.VendorSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VendorRepositoryCustom {
    Page<VendorResponse> searchPageComplex(VendorSearchCondition condition, Pageable pageable);
}
