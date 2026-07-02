package erp.backEnd.dto.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** 대량 공급사 "미리보기" 응답(저장하지 않음). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkVendorPreviewResponse {

    private int totalRows;
    private int validRows;
    private int errorRows;
    private boolean confirmable;
    private List<PreviewRow> rows = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviewRow {
        private int rowNo;
        private String vendorCode;
        private String vendorName;
        private boolean valid;
        private String error;
    }
}
