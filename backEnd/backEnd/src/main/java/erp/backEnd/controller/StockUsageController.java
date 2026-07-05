package erp.backEnd.controller;

import erp.backEnd.dto.po.StockUsageCreateRequest;
import erp.backEnd.dto.po.StockUsageRejectRequest;
import erp.backEnd.dto.po.StockUsageResponse;
import erp.backEnd.dto.po.StockUsageSearchCondition;
import erp.backEnd.entity.StockUsage;
import erp.backEnd.enumeration.UsageStatus;
import erp.backEnd.service.StockUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "재고 사용(출고)", description = "재고 사용 신청·조회·승인/반려 API")
@RestController("stockUsage")
@RequestMapping("/stock-usage")
@RequiredArgsConstructor
public class StockUsageController {

    private final StockUsageService stockUsageService;

    @Operation(summary = "사용 상태 코드 목록", description = "재고 사용 상태(코드/라벨) 목록을 반환한다.")
    @GetMapping("/statuses")
    public ResponseEntity<List<Map<String, String>>> getStatuses() {
        List<Map<String, String>> statuses = Arrays.stream(UsageStatus.values())
                .map(s -> Map.of("code", s.getCode(), "label", s.getLabel()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(statuses);
    }

    @Operation(summary = "재고 사용 목록 조회(검색·페이징)", description = "검색 조건으로 재고 사용 신청을 페이징 조회한다.")
    @GetMapping("/list")
    public ResponseEntity<Page<StockUsageResponse>> list(StockUsageSearchCondition condition, Pageable pageable) {
        return ResponseEntity.ok(stockUsageService.findSearchPage(condition, pageable));
    }

    @Operation(summary = "재고 사용 신청", description = "재고 사용(출고)을 신청한다. 승인 시 재고가 차감된다.")
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody StockUsageCreateRequest request) {
        StockUsage saved = stockUsageService.create(request);
        return ResponseEntity.ok(saved.getId());
    }

    @Operation(summary = "재고 사용 상세 조회", description = "재고 사용 신청 단건의 상세 정보를 조회한다.")
    @GetMapping("/{id}")
    public ResponseEntity<StockUsageResponse> getDetail(
            @Parameter(description = "재고 사용 신청 ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(stockUsageService.getDetail(id));
    }

    @Operation(summary = "재고 사용 승인", description = "재고 사용 신청을 승인하고 재고를 차감한다. 관리자(ADMIN) 권한 필요.")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(
            @Parameter(description = "재고 사용 신청 ID", example = "1") @PathVariable Long id) {
        stockUsageService.approve(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "재고 사용 반려", description = "재고 사용 신청을 반려하고 사유를 기록한다. 관리자(ADMIN) 권한 필요.")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(
            @Parameter(description = "재고 사용 신청 ID", example = "1") @PathVariable Long id,
            @RequestBody StockUsageRejectRequest req) {
        stockUsageService.reject(id, req.getReason());
        return ResponseEntity.ok().build();
    }
}
