package erp.backEnd.service;

import erp.backEnd.dto.po.BulkReceiptPreviewResponse;
import erp.backEnd.dto.po.BulkReceiptRequest;
import erp.backEnd.dto.po.BulkReceiptResponse;
import erp.backEnd.dto.po.ReceiptCreateRequest;
import erp.backEnd.dto.po.ReceiptSummaryResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ReceiptService {

    void createReceipt(Long poId, ReceiptCreateRequest req);

    ReceiptSummaryResponse getSummary(Long poId);

    /**
     * 대량 입고 저장(JSON). JDBC batch insert 로 헤더/라인을 묶어서 저장한다.
     * 검증 오류가 하나라도 있으면 전체 롤백(아무것도 저장하지 않음).
     */
    BulkReceiptResponse bulkCreate(BulkReceiptRequest req);

    /**
     * [2단계 - 미리보기] 엑셀을 파싱+검증만 하고 저장하지 않는다.
     * 행별 정상/오류 여부를 반환하여 프론트에서 오류 행을 표시하고,
     * 오류가 하나라도 있으면 확정(저장)을 막는 데 쓴다.
     */
    BulkReceiptPreviewResponse bulkPreview(MultipartFile file);

    /**
     * [2단계 - 확정 저장] 엑셀을 다시 파싱+검증한 뒤 batch insert 로 저장한다.
     * 미리보기와 동일한 검증을 수행하며, 오류가 하나라도 있으면 전체 롤백한다(방어적 재검증).
     */
    BulkReceiptResponse bulkUpload(MultipartFile file);
}
