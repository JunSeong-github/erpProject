package erp.backEnd.dto.po;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
public class StockResponse {
    private Long itemId;            // 품목 PK
    private String itemCode;        // 품목코드
    private String itemName;        // 품목명
    private BigDecimal standardPrice; // 기준단가
    private Long stockQty;          // 현재 재고수량(누적 입고수량 합계)

    @QueryProjection
    public StockResponse(Long itemId, String itemCode, String itemName, BigDecimal standardPrice, Long stockQty) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.standardPrice = standardPrice;
        this.stockQty = stockQty;
    }
}
