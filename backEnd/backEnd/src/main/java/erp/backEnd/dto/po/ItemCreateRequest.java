package erp.backEnd.dto.po;

import com.querydsl.core.annotations.QueryProjection;
import erp.backEnd.enumeration.PoStatus;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class ItemCreateRequest {

    private String itemCode;

    private String itemName;

    private BigDecimal standardPrice;

    @QueryProjection
    public ItemCreateRequest(String itemCode, BigDecimal standardPrice, String itemName) {
        this.itemCode = itemCode;
        this.standardPrice = standardPrice;
        this.itemName = itemName;
    }

}
