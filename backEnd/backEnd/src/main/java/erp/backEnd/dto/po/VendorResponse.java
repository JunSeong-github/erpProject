package erp.backEnd.dto.po;

import com.querydsl.core.annotations.QueryProjection;
import erp.backEnd.entity.Vendor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
@NoArgsConstructor
public class VendorResponse {
    private String vendorCode;
    private String vendorName;

    @QueryProjection
    public VendorResponse(String vendorCode, String vendorName) {
        this.vendorCode = vendorCode;
        this.vendorName = vendorName;
    }

    public static VendorResponse toDto(Vendor vendor) {
        return VendorResponse.builder()
                .vendorCode(vendor.getVendorCode())
                .vendorName(vendor.getVendorName())
                .build();
    }

    public static List<VendorResponse> toListDto(List<Vendor> vendors) {
        return vendors.stream().map(VendorResponse::toDto).collect(Collectors.toList());
    }

}
