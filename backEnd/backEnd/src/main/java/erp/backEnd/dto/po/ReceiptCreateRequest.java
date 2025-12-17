package erp.backEnd.dto.po;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ReceiptCreateRequest {
    private LocalDate receiptDate;     // 입고일(없으면 서버에서 오늘로 처리)
    private String remark;             // 입고 헤더 비고
    private List<ReceiptLineCreateRequest> lines = new ArrayList<>();

}