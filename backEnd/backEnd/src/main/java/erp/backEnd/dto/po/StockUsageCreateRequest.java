package erp.backEnd.dto.po;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class StockUsageCreateRequest {
    private Long itemId;        // 품목
    private String purpose;     // 사용용도
    private String usagePlace;  // 사용처
    private Long usageQty;      // 사용량
    private LocalDate usageDate; // 사용일(선택)
    private String remark;      // 비고(선택)
}
