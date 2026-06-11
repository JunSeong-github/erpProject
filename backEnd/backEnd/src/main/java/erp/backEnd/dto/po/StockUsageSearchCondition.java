package erp.backEnd.dto.po;

import lombok.Data;

@Data
public class StockUsageSearchCondition {
    private String itemName; // 품목명
    private String status;   // 상태 코드(REQUESTED/APPROVED/REJECTED)
}
