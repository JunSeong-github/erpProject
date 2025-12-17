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
import org.springframework.transaction.annotation.Transactional;

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
               vendor,
               req.getDeliveryDate(),
//               req.getPoStatus(),
               PoStatus.valueOf("DRAFT"),
               req.getEtc()
       );

       Po savedPo = poRepository.save(po);

       List<PoItem> poItems = req.getLines().stream()
               .map(lineReq -> {


                   Item item = itemRepository.findById(lineReq.getItemId())
                           .orElseThrow(() -> new IllegalArgumentException("품목 없음: " + lineReq.getItemId()));

                   Long quantity = lineReq.getQuantity();
                   BigDecimal unitPrice = lineReq.getUnitPrice();
                   BigDecimal amount = lineReq.getAmount();

                   return PoItem.of(
                           savedPo,
                           item,
                           quantity,
                           unitPrice,
                           amount
                   );
               })
               .toList();

       // 4) 라인 일괄 저장
       poItemRepository.saveAll(poItems);

       return savedPo;

   }

    @Transactional
    @Override
    public void approve(Long poId) {
        Po po = poRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("PO 없음: " + poId));

        if (po.getPoStatus() != PoStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 상태만 승인 가능합니다.");
        }

        po.approve();

    }
    @Override
    @Transactional(readOnly = true)
    public PoResponse getDetail(Long id) {
        Po po = poRepository.findDetail(id);
        if (po == null) {
            throw new IllegalArgumentException("발주를 찾을 수 없습니다. id=" + id);
        }

        return PoResponse.from(po);
    }

    // 🔹 수정 저장
    @Override
    @Transactional
    public void update(Long id, PoCreateRequest req) {

        // 1) 기존 PO 조회
        Po po = poRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PO를 찾을 수 없습니다. id=" + id));

        // 2) 공급사 코드로 Vendor 조회
        Vendor vendor = vendorRepository.findByVendorCode(req.getVendorCode())
                .orElseThrow(() -> new IllegalArgumentException("공급사 없음"));

        // 수정
        po.updateFrom(req, vendor);

        // 4) 기존 라인 삭제
        poItemRepository.deleteByPo(po);

        // 5) 새 라인 생성
        if (req.getLines() != null && !req.getLines().isEmpty()) {

            List<PoItem> poItems = req.getLines().stream()
                    .map(lineReq -> {

                        Long itemId = Long.valueOf(lineReq.getItemId());

                        Item item = itemRepository.getReferenceById(itemId);

                        Long quantity = Long.valueOf(lineReq.getQuantity());
                        BigDecimal unitPrice = lineReq.getUnitPrice();
                        BigDecimal amount = lineReq.getAmount();

                        return PoItem.of(po, item, quantity, unitPrice, amount);
                    })
                    .toList();

            poItemRepository.saveAll(poItems);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Po po = poRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PO 없음: " + id));

        if (po.getPoStatus() != PoStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 상태만 삭제 가능합니다.");
        }

        // 라인 먼저 삭제 후 헤더 삭제
        poItemRepository.deleteByPo(po);
        poRepository.delete(po);

    }


    @Transactional
    public void reject(Long id, String reason) {
        Po po = poRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PO 없음: " + id));

        po.reject(reason);
    }


    @Transactional
    public void startReceiving(Long poId) {
        Po po = poRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("PO 없음"));

        po.startReceiving(); // APPROVED -> ORDERED
    }

}
