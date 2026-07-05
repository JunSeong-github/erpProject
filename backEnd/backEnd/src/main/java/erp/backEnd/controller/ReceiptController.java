package erp.backEnd.controller;

import erp.backEnd.dto.po.BulkReceiptPreviewResponse;
import erp.backEnd.dto.po.BulkReceiptRequest;
import erp.backEnd.dto.po.BulkReceiptResponse;
import erp.backEnd.dto.po.ReceiptCreateRequest;
import erp.backEnd.dto.po.ReceiptSummaryResponse;
import erp.backEnd.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "입고(Receipt)", description = "발주 기준 입고 등록·조회 및 엑셀 대량 입고 API")
@RestController("receipt")
@RequestMapping("/receipt")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @Operation(summary = "입고 등록", description = "특정 발주에 대한 입고를 등록하고 재고를 증가시킨다.")
    @PostMapping("/create/{poId}")
    public ResponseEntity<Void> createReceipt(
            @Parameter(description = "발주 ID", example = "1") @PathVariable Long poId,
            @RequestBody ReceiptCreateRequest req
    ) {
        System.out.println("REQ = " + req);
        receiptService.createReceipt(poId, req);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "입고 현황 요약", description = "발주별 발주수량 대비 입고수량/잔량 요약을 조회한다.")
    @GetMapping("/summary/{poId}")
    public ResponseEntity<ReceiptSummaryResponse> summary(
            @Parameter(description = "발주 ID", example = "1") @PathVariable Long poId) {
        return ResponseEntity.ok(receiptService.getSummary(poId));
    }

    /**
     * 대량 입고 저장(JSON). 여러 발주에 걸친 입고 라인을 JDBC batch 로 한번에 저장한다.
     * 검증 오류가 하나라도 있으면 전체 롤백되고, 응답의 errors 에 행별 사유가 담긴다.
     */
    @Operation(summary = "대량 입고 저장(JSON)", description = "여러 발주에 걸친 입고 라인을 JDBC batch 로 한번에 저장한다. 오류가 하나라도 있으면 전체 롤백된다.")
    @PostMapping("/bulk")
    public ResponseEntity<BulkReceiptResponse> bulkCreate(@RequestBody BulkReceiptRequest req) {
        return ResponseEntity.ok(receiptService.bulkCreate(req));
    }

    /**
     * [2단계 - 미리보기] 엑셀 파일을 파싱+검증만 하고 저장하지 않는다.
     * 행별 정상/오류를 반환하므로, 프론트에서 오류 행을 표시하고 오류가 있으면 확정을 막는다.
     */
    @Operation(summary = "대량 입고 미리보기", description = "엑셀 파일을 파싱·검증만 하고 저장하지 않는다. 행별 정상/오류를 반환한다.")
    @PostMapping(value = "/bulk/preview", consumes = "multipart/form-data")
    public ResponseEntity<BulkReceiptPreviewResponse> bulkPreview(
            @Parameter(description = "입고 엑셀 파일(.xlsx)") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(receiptService.bulkPreview(file));
    }

    /**
     * [2단계 - 확정 저장] 미리보기에서 확인한 엑셀 파일을 다시 파싱+검증한 뒤 batch insert 로 저장한다.
     * 방어적으로 재검증하며, 오류가 하나라도 있으면 전체 롤백된다.
     */
    @Operation(summary = "대량 입고 확정 저장", description = "미리보기에서 확인한 엑셀을 재검증 후 batch insert 로 저장한다. 오류가 하나라도 있으면 전체 롤백된다.")
    @PostMapping(value = "/bulk/upload", consumes = "multipart/form-data")
    public ResponseEntity<BulkReceiptResponse> bulkUpload(
            @Parameter(description = "입고 엑셀 파일(.xlsx)") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(receiptService.bulkUpload(file));
    }

}
