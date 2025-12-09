package erp.backEnd.service;

import erp.backEnd.dto.po.VendorResponse;
import erp.backEnd.entity.Vendor;
import erp.backEnd.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;

    public List<VendorResponse> findVendorAll() {
        List<Vendor> vendorList = vendorRepository.findAll();
        return VendorResponse.toListDto(vendorList);
    }

}
