package erp.backEnd.dto.po;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class PoItemCreateRequest {
    private Long poId;          // FK: Po.id
    private Long itemId;        // FK: Item.id

    private Long quantity;      // 수량
    private BigDecimal unitPrice; // 발주 당시 단가
    private BigDecimal amount;    // 수량 * 단가

    @QueryProjection
    public PoItemCreateRequest(Long poId, Long itemId, Long quantity, BigDecimal unitPrice, BigDecimal amount) {
        this.poId = poId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }

}
