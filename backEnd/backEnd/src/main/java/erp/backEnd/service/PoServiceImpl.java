package erp.backEnd.service;

import erp.backEnd.dto.po.PoCreateRequest;
import erp.backEnd.dto.po.PoResponse;
import erp.backEnd.dto.po.PoSearchCondition;
import erp.backEnd.entity.Po;
import erp.backEnd.entity.Vendor;
import erp.backEnd.repository.PoRepository;
import erp.backEnd.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PoServiceImpl implements PoService{

   private final PoRepository poRepository;

   private final VendorRepository vendorRepository;

   public List<PoResponse> findPoList(){
       return poRepository.search();
   }

   public Page<PoResponse> findSearchPageComplex(PoSearchCondition poSearchCondition, Pageable pageable){
       return poRepository.searchPageComplex(poSearchCondition, pageable);
   }

   public Po save(PoCreateRequest req) {

       Vendor vendor = vendorRepository.findByVendorCode(req.getVendorCode())
               .orElseThrow(() -> new IllegalArgumentException("공급사 없음"));

       Po po = Po.of(
               vendor,                  // FK + 연관관계
               req.getDeliveryDate(),
               req.getPoStatus(),
               req.getEtc()
       );

       return poRepository.save(po);

//       Po po = Po.of(poCreateRequest.getDeliveryDate(), poCreateRequest.getPoStatus());
//       return poRepository.save(po);
   }
}
