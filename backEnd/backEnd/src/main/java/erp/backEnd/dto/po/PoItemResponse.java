package erp.backEnd.dto.po;

import com.querydsl.core.annotations.QueryProjection;
import erp.backEnd.entity.PoItem;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@Builder
public class PoItemResponse {
    private Long id;            // PoItem PK
    private Long poId;          // FK: Po.id
    private Long itemId;        // FK: Item.id

    private Long quantity;      // 수량
    private BigDecimal unitPrice; // 발주 당시 단가
    private BigDecimal amount;    // 수량 * 단가

    @QueryProjection
    public PoItemResponse(Long id,Long poId, Long itemId, Long quantity, BigDecimal unitPrice, BigDecimal amount) {
        this.id = id;
        this.poId = poId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }

    public static PoItemResponse from(PoItem entity) {
        return PoItemResponse.builder()
                .itemId(entity.getItem().getId())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .amount(entity.getAmount())
                .build();
    }

}
