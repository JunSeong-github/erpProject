package erp.backEnd.dto.po;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 대량 입고 업로드 요청. 행(라인) 리스트를 담는다.
 */
@Data
@NoArgsConstructor
public class BulkReceiptRequest {
    private List<BulkReceiptRow> rows = new ArrayList<>();
}
