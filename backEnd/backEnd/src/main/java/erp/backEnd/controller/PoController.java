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

    @GetMapping("/list")
    public ResponseEntity<Page<PoResponse>> list(PoSearchCondition poSearchCondition, Pageable pageable) {
        Page<PoResponse> List = poService.findSearchPageComplex(poSearchCondition, pageable);
        return ResponseEntity.ok(List);
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody PoCreateRequest poCreateRequest) {
        Po save = poService.save(poCreateRequest);
        return ResponseEntity.ok(save);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        poService.approve(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id, @RequestBody PoRejectRequest req) {
        poService.reject(id, req.getReason());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        poService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PoResponse> getDetail(@PathVariable Long id) {
        PoResponse response = poService.getDetail(id);
        return ResponseEntity.ok(response);
    }

    // 수정 저장
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @RequestBody PoCreateRequest poCreateRequest
    ) {
        poService.update(id, poCreateRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/startReceiving/{id}")
    public ResponseEntity<Void> startReceiving(@PathVariable Long id) {
        poService.startReceiving(id);
        return ResponseEntity.ok().build();
    }

    /**
     * [대량 발주 - 미리보기] 엑셀 파일을 파싱+검증만 하고 저장하지 않는다.
     * 행별 정상/오류를 반환하므로, 프론트에서 오류 행을 표시하고 오류가 있으면 확정을 막는다.
     */
    @PostMapping(value = "/bulk/preview", consumes = "multipart/form-data")
    public ResponseEntity<BulkPoPreviewResponse> bulkPreview(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(poService.bulkPreview(file));
    }

    /**
     * [대량 발주 - 확정 저장] 엑셀 파일을 다시 파싱+검증한 뒤 batch insert 로 저장한다(DRAFT).
     * 오류가 하나라도 있으면 전체 롤백된다.
     */
    @PostMapping(value = "/bulk/upload", consumes = "multipart/form-data")
    public ResponseEntity<BulkPoResponse> bulkUpload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(poService.bulkUpload(file));
    }

}
