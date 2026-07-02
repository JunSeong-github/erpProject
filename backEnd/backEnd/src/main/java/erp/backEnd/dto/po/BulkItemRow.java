package erp.backEnd.dto.po;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 대량 품목 업로드의 한 행(= 품목 1건). */
@Data
@NoArgsConstructor
public class BulkItemRow {
    private String itemCode;        // 품목코드 (필수, 중복불가)
    private String itemName;        // 품목이름 (필수, 중복불가)
    private BigDecimal standardPrice; // 품목가격 (필수, 0 이상)
}
