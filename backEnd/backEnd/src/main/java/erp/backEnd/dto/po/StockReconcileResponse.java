package erp.backEnd.dto.po;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 재고 대사(reconciliation) 결과.
 * columnStock(정답값 stock_qty) 과 aggregatedStock(입고합-승인사용합) 을 비교한다.
 * matched=false 이면 어딘가에서 재고 컬럼 동기화가 누락된 것이므로 점검이 필요하다.
 */
@Builder
@Data
@NoArgsConstructor
public class StockReconcileResponse {
    private Long itemId;
    private String itemCode;
    private String itemName;
    private long columnStock;      // stock_qty 컬럼값(정답으로 사용하는 값)
    private long aggregatedStock;  // 원장 집계값(입고합 - 승인사용합)
    private boolean matched;       // 두 값 일치 여부
    private long diff;             // columnStock - aggregatedStock

    public StockReconcileResponse(Long itemId, String itemCode, String itemName,
                                  long columnStock, long aggregatedStock, boolean matched, long diff) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.columnStock = columnStock;
        this.aggregatedStock = aggregatedStock;
        this.matched = matched;
        this.diff = diff;
    }
}
