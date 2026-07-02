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
    private Long poItemId;            // PoItem PK
    private Long poId;          // FK: Po.id
    private Long itemId;        // FK: Item.id
    private String itemCode;    // 품목코드
    private String itemName;

    private Long quantity;      // 수량
    private BigDecimal unitPrice; // 발주 당시 단가
    private BigDecimal amount;    // 수량 * 단가

    @QueryProjection
    public PoItemResponse(Long poItemId,Long poId, Long itemId, String itemCode, String itemName,Long quantity, BigDecimal unitPrice, BigDecimal amount) {
        this.poItemId = poItemId;
        this.poId = poId;
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }

    public static PoItemResponse from(PoItem entity) {
        return PoItemResponse.builder()
                .poItemId(entity.getPoItemId())
                .itemId(entity.getItem().getId())
                .itemCode(entity.getItem().getItemCode())
                .itemName(entity.getItem().getItemName())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .amount(entity.getAmount())
                .build();
    }

}
