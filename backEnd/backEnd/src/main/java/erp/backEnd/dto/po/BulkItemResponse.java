package erp.backEnd.dto.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** 대량 품목 업로드 결과. 오류가 하나라도 있으면 전체 롤백. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkItemResponse {
    private int totalRows;
    private int successRows;   // 저장된 품목 수
    private int failRows;
    private List<RowError> errors = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private int row;
        private String message;
    }
}
