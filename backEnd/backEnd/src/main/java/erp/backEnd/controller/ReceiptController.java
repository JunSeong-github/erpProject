package erp.backEnd.controller;

import erp.backEnd.dto.po.BulkReceiptPreviewResponse;
import erp.backEnd.dto.po.BulkReceiptRequest;
import erp.backEnd.dto.po.BulkReceiptResponse;
import erp.backEnd.dto.po.ReceiptCreateRequest;
import erp.backEnd.dto.po.ReceiptSummaryResponse;
import erp.backEnd.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController("receipt")
@RequestMapping("/receipt")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping("/create/{poId}")
    public ResponseEntity<Void> createReceipt(
            @PathVariable Long poId,
            @RequestBody ReceiptCreateRequest req
    ) {
        System.out.println("REQ = " + req);
        receiptService.createReceipt(poId, req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary/{poId}")
    public ResponseEntity<ReceiptSummaryResponse> summary(@PathVariable Long poId) {
        return ResponseEntity.ok(receiptService.getSummary(poId));
    }

    /**
     * 대량 입고 저장(JSON). 여러 발주에 걸친 입고 라인을 JDBC batch 로 한번에 저장한다.
     * 검증 오류가 하나라도 있으면 전체 롤백되고, 응답의 errors 에 행별 사유가 담긴다.
     */
    @PostMapping("/bulk")
    public ResponseEntity<BulkReceiptResponse> bulkCreate(@RequestBody BulkReceiptRequest req) {
        return ResponseEntity.ok(receiptService.bulkCreate(req));
    }

    /**
     * [2단계 - 미리보기] 엑셀 파일을 파싱+검증만 하고 저장하지 않는다.
     * 행별 정상/오류를 반환하므로, 프론트에서 오류 행을 표시하고 오류가 있으면 확정을 막는다.
     */
    @PostMapping(value = "/bulk/preview", consumes = "multipart/form-data")
    public ResponseEntity<BulkReceiptPreviewResponse> bulkPreview(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(receiptService.bulkPreview(file));
    }

    /**
     * [2단계 - 확정 저장] 미리보기에서 확인한 엑셀 파일을 다시 파싱+검증한 뒤 batch insert 로 저장한다.
     * 방어적으로 재검증하며, 오류가 하나라도 있으면 전체 롤백된다.
     */
    @PostMapping(value = "/bulk/upload", consumes = "multipart/form-data")
    public ResponseEntity<BulkReceiptResponse> bulkUpload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(receiptService.bulkUpload(file));
    }

}
