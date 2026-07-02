package erp.backEnd.dto.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 대량 품목 "미리보기" 응답(저장하지 않음). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkItemPreviewResponse {

    private int totalRows;
    private int validRows;
    private int errorRows;
    private boolean confirmable; // errorRows == 0 && totalRows > 0
    private List<PreviewRow> rows = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviewRow {
        private int rowNo;
        private String itemCode;
        private String itemName;
        private BigDecimal standardPrice;
        private boolean valid;
        private String error;
    }
}
