package erp.backEnd.service;

import erp.backEnd.dto.po.ReceiptCreateRequest;
import erp.backEnd.dto.po.ReceiptSummaryResponse;
import erp.backEnd.entity.Receipt;

import java.util.Optional;

public interface ReceiptService {

    void createReceipt(Long poId, ReceiptCreateRequest req);

    ReceiptSummaryResponse getSummary(Long poId);
}
