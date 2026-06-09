package erp.backEnd.dto.po;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VendorCreateRequest {
    private String vendorCode;

    private String vendorName;

    @QueryProjection
    public VendorCreateRequest(String vendorCode, String vendorName) {
        this.vendorCode = vendorCode;
        this.vendorName = vendorName;
    }
}
