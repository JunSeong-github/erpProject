package erp.backEnd.controller;

import erp.backEnd.dto.po.StockUsageCreateRequest;
import erp.backEnd.dto.po.StockUsageRejectRequest;
import erp.backEnd.dto.po.StockUsageResponse;
import erp.backEnd.dto.po.StockUsageSearchCondition;
import erp.backEnd.entity.StockUsage;
import erp.backEnd.enumeration.UsageStatus;
import erp.backEnd.service.StockUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController("stockUsage")
@RequestMapping("/stock-usage")
@RequiredArgsConstructor
public class StockUsageController {

    private final StockUsageService stockUsageService;

    @GetMapping("/statuses")
    public ResponseEntity<List<Map<String, String>>> getStatuses() {
        List<Map<String, String>> statuses = Arrays.stream(UsageStatus.values())
                .map(s -> Map.of("code", s.getCode(), "label", s.getLabel()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/list")
    public ResponseEntity<Page<StockUsageResponse>> list(StockUsageSearchCondition condition, Pageable pageable) {
        return ResponseEntity.ok(stockUsageService.findSearchPage(condition, pageable));
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody StockUsageCreateRequest request) {
        StockUsage saved = stockUsageService.create(request);
        return ResponseEntity.ok(saved.getId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockUsageResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(stockUsageService.getDetail(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        stockUsageService.approve(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id, @RequestBody StockUsageRejectRequest req) {
        stockUsageService.reject(id, req.getReason());
        return ResponseEntity.ok().build();
    }
}
