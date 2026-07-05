package erp.backEnd.controller;

import erp.backEnd.dto.po.BulkPoPreviewResponse;
import erp.backEnd.dto.po.BulkPoResponse;
import erp.backEnd.dto.po.PoCreateRequest;
import erp.backEnd.dto.po.PoRejectRequest;
import erp.backEnd.dto.po.PoResponse;
import erp.backEnd.dto.po.PoSearchCondition;
import erp.backEnd.entity.Po;
import erp.backEnd.enumeration.PoStatus;
import erp.backEnd.service.PoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "발주(PO)", description = "발주 등록·조회·승인/반려·삭제 및 엑셀 대량 발주 API")
@RestController("po")
@RequestMapping("/po")
@RequiredArgsConstructor
public class PoController {

    private final PoService poService;

//    @GetMapping("list")
//    public ResponseEntity<List<PoResponse>> findPoList() {
//        List<PoResponse> poList = poService.findPoList();
//        return ResponseEntity.ok(poList);
//    }

    @Operation(summary = "발주 상태 코드 목록", description = "발주 상태(코드/라벨) 목록을 반환한다. 프론트 필터·드롭다운 구성용.")
    @GetMapping("/statuses")
    public ResponseEntity<List<Map<String, String>>> getPoStatuses() {
        List<Map<String, String>> statuses = Arrays.stream(PoStatus.values())
                .map(status -> Map.of(
                        "code", status.getCode(),
                        "label", status.getLabel()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(statuses);
    }

    @Operation(summary = "발주 목록 조회(검색·페이징)", description = "검색 조건(상태·거래처·기간 등)으로 발주를 페이징 조회한다.")
    @GetMapping("/list")
    public ResponseEntity<Page<PoResponse>> list(PoSearchCondition poSearchCondition, Pageable pageable) {
        Page<PoResponse> List = poService.findSearchPageComplex(poSearchCondition, pageable);
        return ResponseEntity.ok(List);
    }

    @Operation(summary = "발주 등록", description = "신규 발주를 DRAFT 상태로 등록한다.")
    @PostMapping("/create")
    public ResponseEntity<PoResponse> create(@RequestBody PoCreateRequest poCreateRequest) {
        Po save = poService.save(poCreateRequest);
        // Po 엔티티를 그대로 반환하면 양방향 연관관계(Po↔Vendor↔Po↔PoItem…)가 무한재귀 직렬화되어
        // HttpMessageNotWritableException 이 발생한다. PoResponse DTO 로 변환해 반환한다.
        return ResponseEntity.ok(PoResponse.from(save));
    }

    @Operation(summary = "발주 승인", description = "발주를 승인한다. 관리자(ADMIN) 권한 필요.")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(
            @Parameter(description = "발주 ID", example = "1") @PathVariable Long id) {
        poService.approve(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "발주 반려", description = "발주를 반려하고 사유를 기록한다. 관리자(ADMIN) 권한 필요.")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(
            @Parameter(description = "발주 ID", example = "1") @PathVariable Long id,
            @RequestBody PoRejectRequest req) {
        poService.reject(id, req.getReason());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "발주 삭제", description = "발주를 삭제한다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "발주 ID", example = "1") @PathVariable Long id) {
        poService.delete(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "발주 상세 조회", description = "발주 단건의 상세 정보(품목·수량·상태 등)를 조회한다.")
    @GetMapping("/{id}")
    public ResponseEntity<PoResponse> getDetail(
            @Parameter(description = "발주 ID", example = "1") @PathVariable Long id) {
        PoResponse response = poService.getDetail(id);
        return ResponseEntity.ok(response);
    }

    // 수정 저장
    @Operation(summary = "발주 수정", description = "발주 내용을 수정 저장한다.")
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @RequestBody PoCreateRequest poCreateRequest
    ) {
        poService.update(id, poCreateRequest);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "입고 시작", description = "승인된 발주를 입고 진행 상태로 전환한다.")
    @PostMapping("/startReceiving/{id}")
    public ResponseEntity<Void> startReceiving(
            @Parameter(description = "발주 ID", example = "1") @PathVariable Long id) {
        poService.startReceiving(id);
        return ResponseEntity.ok().build();
    }

    /**
     * [대량 발주 - 미리보기] 엑셀 파일을 파싱+검증만 하고 저장하지 않는다.
     * 행별 정상/오류를 반환하므로, 프론트에서 오류 행을 표시하고 오류가 있으면 확정을 막는다.
     */
    @Operation(summary = "대량 발주 미리보기", description = "엑셀 파일을 파싱·검증만 하고 저장하지 않는다. 행별 정상/오류를 반환한다.")
    @PostMapping(value = "/bulk/preview", consumes = "multipart/form-data")
    public ResponseEntity<BulkPoPreviewResponse> bulkPreview(
            @Parameter(description = "발주 엑셀 파일(.xlsx)") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(poService.bulkPreview(file));
    }

    /**
     * [대량 발주 - 확정 저장] 엑셀 파일을 다시 파싱+검증한 뒤 batch insert 로 저장한다(DRAFT).
     * 오류가 하나라도 있으면 전체 롤백된다.
     */
    @Operation(summary = "대량 발주 확정 저장", description = "엑셀을 재검증 후 batch insert 로 저장(DRAFT)한다. 오류가 하나라도 있으면 전체 롤백된다.")
    @PostMapping(value = "/bulk/upload", consumes = "multipart/form-data")
    public ResponseEntity<BulkPoResponse> bulkUpload(
            @Parameter(description = "발주 엑셀 파일(.xlsx)") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(poService.bulkUpload(file));
    }

}
