package erp.backEnd.dto.po;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReceiptLineCreateRequest {
    private Long poItemId;     // PO 라인 PK (반드시 필요)
    private Long receivedQty;  // 실제 입고수량 (nullable 허용 -> 서버에서 0 처리)
    private String lineRemark; // 라인 비고

    public Long getPoItemId() {
        return poItemId;
    }

    public void setPoItemId(Long poItemId) {
        this.poItemId = poItemId;
    }

    public Long getReceivedQty() {
        return receivedQty;
    }

    public void setReceivedQty(Long receivedQty) {
        this.receivedQty = receivedQty;
    }

    public String getLineRemark() {
        return lineRemark;
    }

    public void setLineRemark(String lineRemark) {
        this.lineRemark = lineRemark;
    }
}
