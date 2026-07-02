package erp.backEnd.dto.po;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 대량 발주 업로드의 한 행(= 발주 라인 1건).
 * 같은 발주그룹(groupKey)으로 묶이거나, groupKey 가 없으면 (공급사코드, 납기일) 기준으로
 * 하나의 발주(헤더)로 묶인다.
 */
@Data
@NoArgsConstructor
public class BulkPoRow {
    private String vendorCode;     // 공급사코드 (필수)
    private String itemCode;       // 품목코드 (필수)
    private Long quantity;         // 수량 (필수, 1 이상)
    private BigDecimal unitPrice;  // 단가 (없으면 품목 표준가 사용)
    private LocalDate deliveryDate;// 납기 요청일 (필수)
    private String etc;            // 발주 비고(헤더, 그룹 내 첫 행 값 사용)
    private String groupKey;       // 발주그룹(같은 값끼리 한 발주로 묶음, 없으면 공급사+납기일로 묶음)
}
