package erp.backEnd.controller;

import erp.backEnd.dto.po.BulkVendorPreviewResponse;
import erp.backEnd.dto.po.BulkVendorResponse;
import erp.backEnd.dto.po.VendorCreateRequest;
import erp.backEnd.dto.po.VendorResponse;
import erp.backEnd.dto.po.VendorSearchCondition;
import erp.backEnd.entity.Vendor;
import erp.backEnd.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController("vendor")
@RequestMapping("vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    // 공급사 전체 목록 (발주작성 화면 드롭다운 등에서 사용)
    @GetMapping
    public List<VendorResponse> getVendors() {
        return vendorService.findVendorAll();
    }

    // 공급사 목록 (검색조건 + 페이징)
    @GetMapping("/list")
    public ResponseEntity<Page<VendorResponse>> list(VendorSearchCondition vendorSearchCondition, Pageable pageable) {
        Page<VendorResponse> list = vendorService.findSearchPageComplex(vendorSearchCondition, pageable);
        return ResponseEntity.ok(list);
    }

    // 공급사코드 중복 체크
    @GetMapping("/checkDuplicate")
    public ResponseEntity<Boolean> checkDuplicate(@RequestParam String vendorCode) {
        boolean isDuplicate = vendorService.existsByVendorCode(vendorCode);
        return ResponseEntity.ok(isDuplicate);
    }

    // 공급사명 중복 체크
    @GetMapping("/checkDuplicateName")
    public ResponseEntity<Boolean> checkDuplicateName(@RequestParam String vendorName) {
        boolean isDuplicate = vendorService.existsByVendorName(vendorName);
        return ResponseEntity.ok(isDuplicate);
    }

    // 공급사 등록
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody VendorCreateRequest vendorCreateRequest) {
        Vendor save = vendorService.save(vendorCreateRequest);
        return ResponseEntity.ok(save);
    }

    // 공급사 수정 (공급사명)
    @PatchMapping("/update/{vendorCode}")
    public ResponseEntity<Void> update(
            @PathVariable String vendorCode,
            @RequestBody VendorCreateRequest vendorCreateRequest
    ) {
        vendorService.update(vendorCode, vendorCreateRequest);
        return ResponseEntity.ok().build();
    }

    // 공급사 상세
    @GetMapping("/{vendorCode}")
    public ResponseEntity<VendorResponse> getDetail(@PathVariable String vendorCode) {
        VendorResponse response = vendorService.getDetail(vendorCode);
        return ResponseEntity.ok(response);
    }

    // 공급사 삭제
    @DeleteMapping("/{vendorCode}")
    public ResponseEntity<Void> delete(@PathVariable String vendorCode) {
        vendorService.delete(vendorCode);
        return ResponseEntity.ok().build();
    }

    // [대량 공급사 - 미리보기] 파싱+검증만, 저장 X
    @PostMapping(value = "/bulk/preview", consumes = "multipart/form-data")
    public ResponseEntity<BulkVendorPreviewResponse> bulkPreview(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(vendorService.bulkPreview(file));
    }

    // [대량 공급사 - 확정 저장] 오류 없으면 batch insert, 있으면 전체 롤백
    @PostMapping(value = "/bulk/upload", consumes = "multipart/form-data")
    public ResponseEntity<BulkVendorResponse> bulkUpload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(vendorService.bulkUpload(file));
    }

}
