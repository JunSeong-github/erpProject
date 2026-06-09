package erp.backEnd.service;

import erp.backEnd.dto.po.VendorCreateRequest;
import erp.backEnd.dto.po.VendorResponse;
import erp.backEnd.dto.po.VendorSearchCondition;
import erp.backEnd.entity.Vendor;
import erp.backEnd.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;

    public List<VendorResponse> findVendorAll() {
        List<Vendor> vendorList = vendorRepository.findAll();
        return VendorResponse.toListDto(vendorList);
    }

    @Override
    public Page<VendorResponse> findSearchPageComplex(VendorSearchCondition condition, Pageable pageable) {
        return vendorRepository.searchPageComplex(condition, pageable);
    }

    @Override
    public Boolean existsByVendorCode(String vendorCode) {
        return vendorRepository.existsByVendorCode(vendorCode);
    }

    @Override
    @Transactional
    public Vendor save(VendorCreateRequest req) {

        if (vendorRepository.existsByVendorCode(req.getVendorCode())) {
            throw new IllegalArgumentException("이미 존재하는 공급사코드입니다.");
        }

        Vendor vendor = Vendor.of(
                req.getVendorCode(),
                req.getVendorName()
        );

        return vendorRepository.save(vendor);
    }

    @Override
    @Transactional
    public void update(String vendorCode, VendorCreateRequest req) {

        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("저장된 공급사를 찾을 수 없습니다."));

        vendor.updateForm(req);
    }

    @Override
    @Transactional
    public VendorResponse getDetail(String vendorCode) {

        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("저장된 공급사를 찾을 수 없습니다."));

        return VendorResponse.toDto(vendor);
    }

    @Override
    @Transactional
    public void delete(String vendorCode) {

        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("저장된 공급사를 찾을 수 없습니다."));

        vendorRepository.deleteById(vendorCode);
    }

}
