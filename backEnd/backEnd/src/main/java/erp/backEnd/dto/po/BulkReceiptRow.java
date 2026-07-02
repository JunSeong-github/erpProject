package erp.backEnd.dto.po;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 대량 입고 업로드의 한 행(= 입고 라인 1건).
 * 엑셀 한 행이 이 DTO 하나에 매핑되는 것을 상정한다.
 * 같은 (poId, receiptDate) 를 가진 행들은 서버에서 하나의 입고건(헤더)으로 묶인다.
 */
@Data
@NoArgsConstructor
public class BulkReceiptRow {
    private Long poId;          // 발주번호(PK)
    private Long poItemId;      // 발주 라인 PK (품목코드로 매핑 시 서버가 해석해 채움)
    private String itemCode;    // 품목코드(발주라인번호 미지정 시 이 값으로 발주 라인을 찾음)
    private Long receivedQty;   // 실제 입고수량
    private LocalDate receiptDate; // 입고일(없으면 서버에서 오늘로 처리)
    private String remark;      // 입고 헤더 비고(같은 헤더로 묶일 경우 그룹 내 첫 행 값 사용)
    private String lineRemark;  // 라인 비고
}
