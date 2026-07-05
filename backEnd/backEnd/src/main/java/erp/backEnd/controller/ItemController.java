package erp.backEnd.controller;

import erp.backEnd.dto.po.*;
import erp.backEnd.entity.Item;
import erp.backEnd.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "품목(Item)", description = "품목 등록·조회·수정·삭제, 재고 조회/대사 및 엑셀 대량 등록 API")
@RestController("item")
@RequestMapping("items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    // 품목 전체 목록 (발주작성 화면 드롭다운 등에서 사용)
    @Operation(summary = "품목 전체 목록", description = "전체 품목 목록을 반환한다. 발주 작성 드롭다운 등에서 사용.")
    @GetMapping
    public List<ItemResponse> getItems() {
        return itemService.itemFindAll();
    }

    @Operation(summary = "품목 코드 중복 체크", description = "해당 품목 코드가 이미 존재하면 true 를 반환한다.")
    @GetMapping("/checkDuplicate")
    public ResponseEntity<Boolean> checkDuplicate(
            @Parameter(description = "확인할 품목 코드", example = "I001") @RequestParam String itemCode) {
        boolean isDuplicate = itemService.existsByItemCode(itemCode);
        return ResponseEntity.ok(isDuplicate);
    }

    @Operation(summary = "품목명 중복 체크", description = "해당 품목명이 이미 존재하면 true 를 반환한다.")
    @GetMapping("/checkDuplicateName")
    public ResponseEntity<Boolean> checkDuplicateName(
            @Parameter(description = "확인할 품목명", example = "볼트 M6") @RequestParam String itemName) {
        boolean isDuplicate = itemService.existsByItemName(itemName);
        return ResponseEntity.ok(isDuplicate);
    }

    @Operation(summary = "품목 등록", description = "신규 품목을 등록한다.")
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody ItemCreateRequest itemCreateRequest) {
        Item save = itemService.save(itemCreateRequest);
        return ResponseEntity.ok(save);
    }

    @Operation(summary = "품목 수정", description = "품목 정보를 수정한다.")
    @PatchMapping("/update/{id}")
    public ResponseEntity<Void> update(
            @Parameter(description = "품목 ID", example = "1") @PathVariable Long id,
            @RequestBody ItemCreateRequest itemCreateRequest
    ) {
        itemService.update(id, itemCreateRequest);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "품목 상세 조회", description = "품목 단건의 상세 정보를 조회한다.")
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getDetail(
            @Parameter(description = "품목 ID", example = "1") @PathVariable Long id) {
        ItemResponse response = itemService.getDetail(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "품목 사용 여부", description = "해당 품목이 발주/입고에 사용됐는지 반환한다. 사용된 품목은 수정·삭제가 제한된다.")
    @GetMapping("/{id}/in-use")
    public ResponseEntity<Boolean> inUse(
            @Parameter(description = "품목 ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(itemService.isInUse(id));
    }

    @Operation(summary = "품목 삭제", description = "품목을 삭제한다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @Parameter(description = "품목 ID", example = "1") @PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "품목 목록 조회(검색·페이징)", description = "검색 조건으로 품목을 페이징 조회한다.")
    @GetMapping("/list")
    public ResponseEntity<Page<ItemResponse>> list(ItemSearchCondition itemSearchCondition, Pageable pageable) {
        Page<ItemResponse> List = itemService.findSearchPageComplex(itemSearchCondition, pageable);
        return ResponseEntity.ok(List);
    }

    @Operation(summary = "품목 재고 목록 조회", description = "품목별 현재 재고 수량을 검색·페이징 조회한다.")
    @GetMapping("/stock")
    public ResponseEntity<Page<StockResponse>> stock(ItemSearchCondition itemSearchCondition, Pageable pageable) {
        Page<StockResponse> page = itemService.findStockPage(itemSearchCondition, pageable);
        return ResponseEntity.ok(page);
    }

    // 재고 대사(reconciliation): stock_qty 컬럼값 vs 원장 집계값 비교
    @Operation(summary = "재고 대사(reconcile)", description = "품목의 stock_qty 컬럼값과 입출고 원장 집계값을 비교해 불일치 여부를 반환한다.")
    @GetMapping("/{id}/stock/reconcile")
    public ResponseEntity<StockReconcileResponse> reconcileStock(
            @Parameter(description = "품목 ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(itemService.reconcileStock(id));
    }

    // [대량 품목 - 미리보기] 파싱+검증만, 저장 X
    @Operation(summary = "대량 품목 미리보기", description = "엑셀 파일을 파싱·검증만 하고 저장하지 않는다. 행별 정상/오류를 반환한다.")
    @PostMapping(value = "/bulk/preview", consumes = "multipart/form-data")
    public ResponseEntity<BulkItemPreviewResponse> bulkPreview(
            @Parameter(description = "품목 엑셀 파일(.xlsx)") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(itemService.bulkPreview(file));
    }

    // [대량 품목 - 확정 저장] 오류 없으면 batch insert, 있으면 전체 롤백
    @Operation(summary = "대량 품목 확정 저장", description = "엑셀을 재검증 후 batch insert 로 저장한다. 오류가 하나라도 있으면 전체 롤백된다.")
    @PostMapping(value = "/bulk/upload", consumes = "multipart/form-data")
    public ResponseEntity<BulkItemResponse> bulkUpload(
            @Parameter(description = "품목 엑셀 파일(.xlsx)") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(itemService.bulkUpload(file));
    }

}
