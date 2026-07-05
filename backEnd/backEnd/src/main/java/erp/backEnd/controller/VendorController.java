package erp.backEnd.controller;

import erp.backEnd.dto.po.BulkVendorPreviewResponse;
import erp.backEnd.dto.po.BulkVendorResponse;
import erp.backEnd.dto.po.VendorCreateRequest;
import erp.backEnd.dto.po.VendorResponse;
import erp.backEnd.dto.po.VendorSearchCondition;
import erp.backEnd.entity.Vendor;
import erp.backEnd.service.VendorService;
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

@Tag(name = "공급사(Vendor)", description = "공급사 등록·조회·수정·삭제, 코드/명 중복 체크 및 엑셀 대량 등록 API")
@RestController("vendor")
@RequestMapping("vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    // 공급사 전체 목록 (발주작성 화면 드롭다운 등에서 사용)
    @Operation(summary = "공급사 전체 목록", description = "전체 공급사 목록을 반환한다. 발주 작성 드롭다운 등에서 사용.")
    @GetMapping
    public List<VendorResponse> getVendors() {
        return vendorService.findVendorAll();
    }

    // 공급사 목록 (검색조건 + 페이징)
    @Operation(summary = "공급사 목록 조회(검색·페이징)", description = "검색 조건으로 공급사를 페이징 조회한다.")
    @GetMapping("/list")
    public ResponseEntity<Page<VendorResponse>> list(VendorSearchCondition vendorSearchCondition, Pageable pageable) {
        Page<VendorResponse> list = vendorService.findSearchPageComplex(vendorSearchCondition, pageable);
        return ResponseEntity.ok(list);
    }

    // 공급사코드 중복 체크
    @Operation(summary = "공급사 코드 중복 체크", description = "해당 공급사 코드가 이미 존재하면 true 를 반환한다.")
    @GetMapping("/checkDuplicate")
    public ResponseEntity<Boolean> checkDuplicate(
            @Parameter(description = "확인할 공급사 코드", example = "V001") @RequestParam String vendorCode) {
        boolean isDuplicate = vendorService.existsByVendorCode(vendorCode);
        return ResponseEntity.ok(isDuplicate);
    }

    // 공급사명 중복 체크
    @Operation(summary = "공급사명 중복 체크", description = "해당 공급사명이 이미 존재하면 true 를 반환한다.")
    @GetMapping("/checkDuplicateName")
    public ResponseEntity<Boolean> checkDuplicateName(
            @Parameter(description = "확인할 공급사명", example = "한빛상사") @RequestParam String vendorName) {
        boolean isDuplicate = vendorService.existsByVendorName(vendorName);
        return ResponseEntity.ok(isDuplicate);
    }

    // 공급사 등록
    @Operation(summary = "공급사 등록", description = "신규 공급사를 등록한다.")
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody VendorCreateRequest vendorCreateRequest) {
        Vendor save = vendorService.save(vendorCreateRequest);
        return ResponseEntity.ok(save);
    }

    // 공급사 수정 (공급사명)
    @Operation(summary = "공급사 수정", description = "공급사 코드로 대상을 찾아 정보(공급사명 등)를 수정한다.")
    @PatchMapping("/update/{vendorCode}")
    public ResponseEntity<Void> update(
            @Parameter(description = "공급사 코드", example = "V001") @PathVariable String vendorCode,
            @RequestBody VendorCreateRequest vendorCreateRequest
    ) {
        vendorService.update(vendorCode, vendorCreateRequest);
        return ResponseEntity.ok().build();
    }

    // 공급사 상세
    @Operation(summary = "공급사 상세 조회", description = "공급사 코드로 단건 상세 정보를 조회한다.")
    @GetMapping("/{vendorCode}")
    public ResponseEntity<VendorResponse> getDetail(
            @Parameter(description = "공급사 코드", example = "V001") @PathVariable String vendorCode) {
        VendorResponse response = vendorService.getDetail(vendorCode);
        return ResponseEntity.ok(response);
    }

    // 공급사 삭제
    @Operation(summary = "공급사 삭제", description = "공급사 코드로 대상을 삭제한다.")
    @DeleteMapping("/{vendorCode}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "공급사 코드", example = "V001") @PathVariable String vendorCode) {
        vendorService.delete(vendorCode);
        return ResponseEntity.ok().build();
    }

    // [대량 공급사 - 미리보기] 파싱+검증만, 저장 X
    @Operation(summary = "대량 공급사 미리보기", description = "엑셀 파일을 파싱·검증만 하고 저장하지 않는다. 행별 정상/오류를 반환한다.")
    @PostMapping(value = "/bulk/preview", consumes = "multipart/form-data")
    public ResponseEntity<BulkVendorPreviewResponse> bulkPreview(
            @Parameter(description = "공급사 엑셀 파일(.xlsx)") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(vendorService.bulkPreview(file));
    }

    // [대량 공급사 - 확정 저장] 오류 없으면 batch insert, 있으면 전체 롤백
    @Operation(summary = "대량 공급사 확정 저장", description = "엑셀을 재검증 후 batch insert 로 저장한다. 오류가 하나라도 있으면 전체 롤백된다.")
    @PostMapping(value = "/bulk/upload", consumes = "multipart/form-data")
    public ResponseEntity<BulkVendorResponse> bulkUpload(
            @Parameter(description = "공급사 엑셀 파일(.xlsx)") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(vendorService.bulkUpload(file));
    }

}
