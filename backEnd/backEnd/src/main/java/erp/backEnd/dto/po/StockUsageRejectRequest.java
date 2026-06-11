package erp.backEnd.dto.po;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StockUsageRejectRequest {
    private String reason;
}
