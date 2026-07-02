package erp.backEnd.controller;

import erp.backEnd.dto.po.*;
import erp.backEnd.entity.Item;
import erp.backEnd.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController("item")
@RequestMapping("items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    // 품목 전체 목록 (발주작성 화면 드롭다운 등에서 사용)
    @GetMapping
    public List<ItemResponse> getItems() {
        return itemService.itemFindAll();
    }

    @GetMapping("/checkDuplicate")
    public ResponseEntity<Boolean> checkDuplicate(@RequestParam String itemCode) {
        boolean isDuplicate = itemService.existsByItemCode(itemCode);
        return ResponseEntity.ok(isDuplicate);
    }

    @GetMapping("/checkDuplicateName")
    public ResponseEntity<Boolean> checkDuplicateName(@RequestParam String itemName) {
        boolean isDuplicate = itemService.existsByItemName(itemName);
        return ResponseEntity.ok(isDuplicate);
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody ItemCreateRequest itemCreateRequest) {
        Item save = itemService.save(itemCreateRequest);
        return ResponseEntity.ok(save);
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @RequestBody ItemCreateRequest itemCreateRequest
    ) {
        itemService.update(id, itemCreateRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getDetail(@PathVariable Long id) {
        ItemResponse response = itemService.getDetail(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity<Page<ItemResponse>> list(ItemSearchCondition itemSearchCondition, Pageable pageable) {
        Page<ItemResponse> List = itemService.findSearchPageComplex(itemSearchCondition, pageable);
        return ResponseEntity.ok(List);
    }

    @GetMapping("/stock")
    public ResponseEntity<Page<StockResponse>> stock(ItemSearchCondition itemSearchCondition, Pageable pageable) {
        Page<StockResponse> page = itemService.findStockPage(itemSearchCondition, pageable);
        return ResponseEntity.ok(page);
    }

    // [대량 품목 - 미리보기] 파싱+검증만, 저장 X
    @PostMapping(value = "/bulk/preview", consumes = "multipart/form-data")
    public ResponseEntity<BulkItemPreviewResponse> bulkPreview(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(itemService.bulkPreview(file));
    }

    // [대량 품목 - 확정 저장] 오류 없으면 batch insert, 있으면 전체 롤백
    @PostMapping(value = "/bulk/upload", consumes = "multipart/form-data")
    public ResponseEntity<BulkItemResponse> bulkUpload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(itemService.bulkUpload(file));
    }

}
