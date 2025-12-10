package erp.backEnd.dto.po;

import com.querydsl.core.annotations.QueryProjection;
import erp.backEnd.enumeration.PoStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class PoCreateRequest {
    private String vendorCode;      // 공급사코드
    private LocalDate deliveryDate; // 납기 요청일
    private String etc;
    private PoStatus poStatus;      // 발주 상태

    private List<PoItemCreateRequest> lines;  // 발주 품목 리스트

    @QueryProjection
    public PoCreateRequest(String vendorCode, LocalDate deliveryDate, String etc, PoStatus poStatus) {
        this.vendorCode = vendorCode;
        this.deliveryDate = deliveryDate;
        this.etc = etc;
        this.poStatus = poStatus;
    }
}
