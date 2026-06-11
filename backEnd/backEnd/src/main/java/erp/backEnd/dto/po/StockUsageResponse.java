package erp.backEnd.dto.po;

import com.querydsl.core.annotations.QueryProjection;
import erp.backEnd.enumeration.UsageStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class StockUsageResponse {
    private Long id;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String purpose;      // 사용용도
    private String usagePlace;   // 사용처
    private Long usageQty;       // 사용량
    private LocalDate usageDate; // 사용일
    private String remark;       // 비고
    private String status;       // 상태 코드
    private String statusLabel;  // 상태 한글명
    private String rejectReason; // 반려사유
    private LocalDateTime createdDate; // 등록일

    @QueryProjection
    public StockUsageResponse(Long id, Long itemId, String itemCode, String itemName,
                              String purpose, String usagePlace, Long usageQty, LocalDate usageDate,
                              String remark, UsageStatus status, String rejectReason,
                              LocalDateTime createdDate) {
        this.id = id;
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.purpose = purpose;
        this.usagePlace = usagePlace;
        this.usageQty = usageQty;
        this.usageDate = usageDate;
        this.remark = remark;
        this.status = status != null ? status.getCode() : null;
        this.statusLabel = status != null ? status.getLabel() : null;
        this.rejectReason = rejectReason;
        this.createdDate = createdDate;
    }
}
