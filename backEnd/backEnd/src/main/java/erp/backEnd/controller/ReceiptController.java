package erp.backEnd.controller;

import erp.backEnd.dto.po.ReceiptCreateRequest;
import erp.backEnd.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("receipt")
@RequestMapping("/receipt")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    /**
     * 입고 등록
     * POST /receipt/create/{poId}
     */
    @PostMapping("/create/{poId}")
    public ResponseEntity<Void> createReceipt(
            @PathVariable Long poId,
            @RequestBody ReceiptCreateRequest req
    ) {
        receiptService.createReceipt(poId, req);
        return ResponseEntity.ok().build();
    }
}
