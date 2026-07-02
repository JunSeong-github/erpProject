package erp.backEnd.dto.po;

import lombok.Data;
import lombok.NoArgsConstructor;

/** 대량 공급사 업로드의 한 행(= 공급사 1건). */
@Data
@NoArgsConstructor
public class BulkVendorRow {
    private String vendorCode;  // 공급사코드 (필수, 중복불가)
    private String vendorName;  // 공급사명 (필수, 중복불가)
}
