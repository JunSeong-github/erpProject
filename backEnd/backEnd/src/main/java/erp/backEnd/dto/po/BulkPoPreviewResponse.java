package erp.backEnd.dto.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 대량 발주 "미리보기" 응답(저장하지 않음).
 * 행별 정상/오류를 담아, 프론트에서 오류 행을 강조 표시하고 오류가 있으면 확정을 막는다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkPoPreviewResponse {

    private int totalRows;   // 전체 데이터 행 수
    private int validRows;   // 정상 행 수
    private int errorRows;   // 오류 행 수
    private int poCount;     // 정상 행들이 묶여 생성될 발주(헤더) 수
    private boolean confirmable; // 저장(확정) 가능 여부 = (errorRows == 0 && totalRows > 0)
    private List<PreviewRow> rows = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviewRow {
        private int rowNo;            // 엑셀 실제 행 번호(1-based)
        private String groupLabel;    // 어떤 발주로 묶이는지 표시용 라벨
        private String vendorCode;
        private String vendorName;    // 확인된 공급사명(없으면 null)
        private String itemCode;
        private String itemName;      // 확인된 품목명(없으면 null)
        private Long quantity;
        private BigDecimal unitPrice; // 미입력 시 품목 표준가로 채워진 값
        private BigDecimal amount;    // 단가 * 수량
        private LocalDate deliveryDate;
        private String etc;
        private boolean valid;
        private String error;         // 오류 사유(정상이면 null)
    }
}
