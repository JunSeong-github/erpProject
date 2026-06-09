package erp.backEnd.service;

import erp.backEnd.dto.po.VendorCreateRequest;
import erp.backEnd.dto.po.VendorResponse;
import erp.backEnd.dto.po.VendorSearchCondition;
import erp.backEnd.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VendorService {

    List<VendorResponse> findVendorAll();

    Page<VendorResponse> findSearchPageComplex(VendorSearchCondition condition, Pageable pageable);

    Vendor save(VendorCreateRequest req);

    Boolean existsByVendorCode(String vendorCode);

    VendorResponse getDetail(String vendorCode);

    void update(String vendorCode, VendorCreateRequest req);

    void delete(String vendorCode);
}
