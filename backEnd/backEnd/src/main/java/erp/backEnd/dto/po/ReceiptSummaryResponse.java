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

}

