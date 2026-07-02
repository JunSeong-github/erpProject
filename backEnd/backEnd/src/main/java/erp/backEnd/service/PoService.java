package erp.backEnd.service;

import erp.backEnd.dto.po.BulkPoPreviewResponse;
import erp.backEnd.dto.po.BulkPoResponse;
import erp.backEnd.dto.po.PoCreateRequest;
import erp.backEnd.dto.po.PoResponse;
import erp.backEnd.dto.po.PoSearchCondition;
import erp.backEnd.entity.Po;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PoService {

    List<PoResponse> findPoList();

    Page<PoResponse> findSearchPageComplex(PoSearchCondition condition, Pageable pageable);

    Po save(PoCreateRequest poCreateRequest);

    void approve(Long poId);

    PoResponse getDetail(Long id);

    void update(Long id, PoCreateRequest req);

    void delete(Long id);

    void reject(Long id, String reason);

    void startReceiving(Long poId);

    /**
     * [대량 발주 - 미리보기] 엑셀을 파싱+검증만 하고 저장하지 않는다.
     * 행별 정상/오류를 반환하여 오류 행 표시 및 확정 차단에 사용.
     */
    BulkPoPreviewResponse bulkPreview(MultipartFile file);

    /**
     * [대량 발주 - 확정 저장] 엑셀을 다시 파싱+검증한 뒤 batch insert 로 저장한다.
     * 오류가 하나라도 있으면 전체 롤백. 생성되는 발주는 DRAFT 상태.
     */
    BulkPoResponse bulkUpload(MultipartFile file);

}
