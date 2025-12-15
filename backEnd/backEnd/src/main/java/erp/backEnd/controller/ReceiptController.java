package erp.backEnd.controller;

import erp.backEnd.dto.po.ReceiptCreateRequest;
import erp.backEnd.dto.po.ReceiptSummaryResponse;
import erp.backEnd.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}
