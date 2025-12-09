package erp.backEnd.dto.po;

import erp.backEnd.enumeration.PoStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PoSearchCondition {
    private String vendorName;      // 공급사명
    private String vendorCode;      // 공급사코드
    private LocalDate deliveryDate; // 납기 요청일
    private PoStatus poStatus;      // 발주 상태
}
