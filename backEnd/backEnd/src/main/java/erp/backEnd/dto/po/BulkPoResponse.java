package erp.backEnd.dto.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 대량 발주 업로드 결과.
 * 정책: 검증 오류가 하나라도 있으면 전체 롤백(아무것도 저장하지 않음).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkPoResponse {
    private int totalRows;      // 요청 행 수
    private int successRows;    // 저장된 라인 수
    private int failRows;       // 검증 실패 행 수
    private int createdPos;     // 생성된 발주(헤더) 수
    private List<RowError> errors = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private int row;        // 1-based 행 번호
        private String message; // 오류 사유
    }
}
