package erp.backEnd.dto.po;

import com.querydsl.core.annotations.QueryProjection;
import erp.backEnd.enumeration.PoStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class PoResponse {
    private Long id;                // Po PK
    private String vendorName;      // 공급사명
    private String vendorCode;      // 공급사코드
    private LocalDate deliveryDate; // 납기 요청일
    private PoStatus poStatus;      // 발주 상태
    private String etc;

    private List<PoItemResponse> items;  // 발주 품목 리스트

    @QueryProjection
    public PoResponse(Long id, String vendorName, String vendorCode, LocalDate deliveryDate, PoStatus poStatus, String etc) {
        this.id = id;
        this.vendorName = vendorName;
        this.vendorCode = vendorCode;
        this.deliveryDate = deliveryDate;
        this.poStatus = poStatus;
        this.etc = etc;
    }
}
