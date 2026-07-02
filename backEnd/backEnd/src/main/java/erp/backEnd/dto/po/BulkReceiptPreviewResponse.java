package erp.backEnd.dto.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 대량 입고 "미리보기" 응답(저장하지 않음).
 * 엑셀을 파싱+검증한 결과를 행 단위로 담아, 프론트에서 오류 행을 강조 표시하고
 * 오류가 하나라도 있으면 확정(저장)을 막는 데 사용한다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkReceiptPreviewResponse {

    private int totalRows;   // 전체 데이터 행 수
    private int validRows;   // 정상 행 수
    private int errorRows;   // 오류 행 수
    private boolean confirmable; // 저장(확정) 가능 여부 = (errorRows == 0 && totalRows > 0)
    private List<PreviewRow> rows = new ArrayList<>();

    /** 미리보기 한 행 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviewRow {
        private int rowNo;            // 엑셀 실제 행 번호(1-based)
        private Long poId;
        private Long poItemId;        // 품목코드로 매핑된 경우 서버가 해석한 발주라인번호가 채워짐
        private String itemCode;      // 입력한 품목코드(있으면)
        private String itemName;      // 검증으로 확인된 품목명(확인 불가 시 null)
        private Long receivedQty;
        private LocalDate receiptDate;
        private String remark;
        private String lineRemark;
        private boolean valid;        // 이 행이 정상인지
        private String error;         // 오류 사유(정상이면 null)
    }
}
