package erp.backEnd.service;

import erp.backEnd.dto.po.PoCreateRequest;
import erp.backEnd.dto.po.PoResponse;
import erp.backEnd.dto.po.PoSearchCondition;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.Po;
import erp.backEnd.entity.PoItem;
import erp.backEnd.entity.Vendor;
import erp.backEnd.enumeration.PoStatus;
import erp.backEnd.repository.ItemRepository;
import erp.backEnd.repository.PoItemRepository;
import erp.backEnd.repository.PoRepository;
import erp.backEnd.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PoServiceImpl implements PoService{

   private final PoRepository poRepository;
   private final PoItemRepository poItemRepository;
   private final ItemRepository itemRepository;

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
//               req.getPoStatus(),
               PoStatus.valueOf("DRAFT"),
               req.getEtc()
       );
       
       //pk가져오깅
       Po savedPo = poRepository.save(po);

       List<PoItem> poItems = req.getLines().stream()
               .map(lineReq -> {

                   // 3-1) 품목 마스터 조회
                   Item item = itemRepository.findById(lineReq.getItemId())
                           .orElseThrow(() -> new IllegalArgumentException("품목 없음: " + lineReq.getItemId()));

                   // 3-2) 수량/단가 파싱 (문자열 → 숫자)
                   Long quantity = lineReq.getQuantity();
                   BigDecimal unitPrice = lineReq.getUnitPrice();
                   BigDecimal amount = lineReq.getAmount();

                   // 3-3) PoItem 엔티티 생성 (정적 팩토리 메서드 가정)
                   return PoItem.of(
                           savedPo,   // FK : 어느 PO에 속한 라인인지
                           item,      // FK : 어떤 품목인지
                           quantity,
                           unitPrice,
                           amount
                   );
               })
               .toList();

       // 4) 라인 일괄 저장
       poItemRepository.saveAll(poItems);

       return savedPo;

//       Po po = Po.of(poCreateRequest.getDeliveryDate(), poCreateRequest.getPoStatus());
//       return poRepository.save(po);
   }
}
