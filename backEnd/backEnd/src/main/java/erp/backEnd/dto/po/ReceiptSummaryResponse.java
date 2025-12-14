package erp.backEnd.dto.po;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class ReceiptSummaryResponse {
    private String remark; // 최근 입고 헤더 비고
    private Map<Long, Long> receivedQtyMap = new HashMap<>(); // poItemId -> 누적입고수량
    private Map<Long, String> lineRemarkMap = new HashMap<>(); // poItemId -> (최근 입고건) 라인비고

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Map<Long, Long> getReceivedQtyMap() { return receivedQtyMap; }
    public void setReceivedQtyMap(Map<Long, Long> receivedQtyMap) { this.receivedQtyMap = receivedQtyMap; }

    public Map<Long, String> getLineRemarkMap() { return lineRemarkMap; }
    public void setLineRemarkMap(Map<Long, String> lineRemarkMap) { this.lineRemarkMap = lineRemarkMap; }
}

