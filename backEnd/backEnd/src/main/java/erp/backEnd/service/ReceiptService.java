package erp.backEnd.service;

import erp.backEnd.dto.po.ReceiptCreateRequest;

public interface ReceiptService {

    void createReceipt(Long poId, ReceiptCreateRequest req);

}
