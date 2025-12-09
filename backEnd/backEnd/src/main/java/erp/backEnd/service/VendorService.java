package erp.backEnd.service;

import erp.backEnd.dto.po.ItemResponse;
import erp.backEnd.dto.po.VendorResponse;
import erp.backEnd.entity.Vendor;

import java.util.List;
import java.util.Optional;

public interface VendorService {

    List<VendorResponse> findVendorAll();
}
