package erp.backEnd.service;

import erp.backEnd.dto.po.BulkVendorPreviewResponse;
import erp.backEnd.dto.po.BulkVendorResponse;
import erp.backEnd.dto.po.VendorCreateRequest;
import erp.backEnd.dto.po.VendorResponse;
import erp.backEnd.dto.po.VendorSearchCondition;
import erp.backEnd.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VendorService {

    List<VendorResponse> findVendorAll();

    Page<VendorResponse> findSearchPageComplex(VendorSearchCondition condition, Pageable pageable);

    Vendor save(VendorCreateRequest req);

    Boolean existsByVendorCode(String vendorCode);

    Boolean existsByVendorName(String vendorName);

    VendorResponse getDetail(String vendorCode);

    void update(String vendorCode, VendorCreateRequest req);

    void delete(String vendorCode);

    /** [대량 공급사 - 미리보기] 엑셀을 파싱+검증만 하고 저장하지 않는다. */
    BulkVendorPreviewResponse bulkPreview(MultipartFile file);

    /** [대량 공급사 - 확정 저장] 재검증 후 batch insert. 오류가 하나라도 있으면 전체 롤백. */
    BulkVendorResponse bulkUpload(MultipartFile file);
}
