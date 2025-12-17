package erp.backEnd.dto.po;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
public class ReceiptLineCreateRequest {
    private Long poItemId;     // PO 라인 PK
    private Long receivedQty;  // 실제 입고수량
    private String lineRemark; // 라인 비고
    private Long orderedQty; // 요청입고수량
    private Long totalReceivedQty; // 누적입고수량

}
