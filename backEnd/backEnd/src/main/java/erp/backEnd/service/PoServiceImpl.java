package erp.backEnd.service;

import erp.backEnd.dto.po.BulkPoPreviewResponse;
import erp.backEnd.dto.po.BulkPoResponse;
import erp.backEnd.dto.po.BulkPoRow;
import erp.backEnd.dto.po.PoCreateRequest;
import erp.backEnd.dto.po.PoResponse;
import erp.backEnd.dto.po.PoSearchCondition;
import erp.backEnd.entity.*;
import erp.backEnd.enumeration.PoStatus;
import erp.backEnd.exception.BusinessException;
import erp.backEnd.exception.ErrorCode;
import erp.backEnd.repository.ItemRepository;
import erp.backEnd.repository.PoBulkRepository;
import erp.backEnd.repository.PoItemRepository;
import erp.backEnd.repository.PoRepository;
import erp.backEnd.repository.VendorRepository;
import erp.backEnd.service.PoExcelParser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PoServiceImpl implements PoService{

   private final PoRepository poRepository;
   private final PoItemRepository poItemRepository;
   private final ItemRepository itemRepository;

   private final VendorRepository vendorRepository;

   private final PoExcelParser poExcelParser;
   private final PoBulkRepository poBulkRepository;

   public List<PoResponse> findPoList(){
       return poRepository.search();
   }

   public Page<PoResponse> findSearchPageComplex(PoSearchCondition poSearchCondition, Pageable pageable){
       return poRepository.searchPageComplex(poSearchCondition, pageable);
   }

   @Override
   @Transactional
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
        Optional<Po> optionalPo = poRepository.findDetail(id);
//        if (po == null) {
//            throw new IllegalArgumentException("발주를 찾을 수 없습니다. id=" + id);
//        }

        Po po = optionalPo.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

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

    @Override
    @Transactional
    public void reject(Long id, String reason) {
        Po po = poRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PO 없음: " + id));

        po.reject(reason);
    }

    @Override
    @Transactional
    public void startReceiving(Long poId) {
        Po po = poRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("PO 없음"));

        po.startReceiving(); // APPROVED -> ORDERED
    }

    // ==================== 대량 발주 업로드(엑셀) ====================

    @Override
    @Transactional(readOnly = true)
    public BulkPoPreviewResponse bulkPreview(MultipartFile file) {
        List<PoExcelParser.ParsedRow> parsed = poExcelParser.parse(file).rows;
        ValidationResult vr = validateAll(parsed);

        BulkPoPreviewResponse resp = new BulkPoPreviewResponse();
        List<BulkPoPreviewResponse.PreviewRow> rows = new ArrayList<>(vr.checks.size());
        int errorCount = 0;
        // 정상 행이 묶일 발주(그룹) 수 계산
        java.util.Set<String> validGroups = new java.util.HashSet<>();
        for (RowCheck c : vr.checks) {
            boolean ok = (c.error == null);
            if (!ok) errorCount++;
            else if (c.valid != null) validGroups.add(c.valid.groupId);

            BulkPoRow raw = c.raw;
            rows.add(new BulkPoPreviewResponse.PreviewRow(
                    c.rowNo, c.groupLabel, raw.getVendorCode(), c.vendorName,
                    raw.getItemCode(), c.itemName, raw.getQuantity(), c.unitPrice, c.amount,
                    raw.getDeliveryDate(), raw.getEtc(), ok, c.error));
        }
        int total = vr.checks.size();
        resp.setTotalRows(total);
        resp.setErrorRows(errorCount);
        resp.setValidRows(total - errorCount);
        resp.setPoCount(validGroups.size());
        resp.setConfirmable(errorCount == 0 && total > 0);
        resp.setRows(rows);
        return resp;
    }

    @Override
    @Transactional
    public BulkPoResponse bulkUpload(MultipartFile file) {
        List<PoExcelParser.ParsedRow> parsed = poExcelParser.parse(file).rows;
        ValidationResult vr = validateAll(parsed);

        List<BulkPoResponse.RowError> errors = new ArrayList<>();
        List<ValidRow> valids = new ArrayList<>();
        for (RowCheck c : vr.checks) {
            if (c.error != null) errors.add(new BulkPoResponse.RowError(c.rowNo, c.error));
            else valids.add(c.valid);
        }
        int total = vr.checks.size();
        if (!errors.isEmpty()) {
            return new BulkPoResponse(total, 0, errors.size(), 0, errors);
        }

        LocalDateTime now = LocalDateTime.now();

        // groupId 기준으로 발주(헤더)로 묶음 (입력 순서 유지)
        LinkedHashMap<String, List<ValidRow>> groups = new LinkedHashMap<>();
        for (ValidRow v : valids) {
            groups.computeIfAbsent(v.groupId, k -> new ArrayList<>()).add(v);
        }
        List<String> groupIds = new ArrayList<>(groups.keySet());

        // 헤더 배치 insert → po_id 회수
        List<PoBulkRepository.PoHeaderRow> headerRows = new ArrayList<>(groupIds.size());
        for (String gid : groupIds) {
            HeaderInfo h = vr.headers.get(gid);
            headerRows.add(new PoBulkRepository.PoHeaderRow(
                    h.deliveryDate, PoStatus.DRAFT.name(), h.etc, h.vendorCode, now));
        }
        List<Long> poIds = poBulkRepository.batchInsertPos(headerRows);

        // 라인 배치 insert
        List<PoBulkRepository.PoItemRow> lineRows = new ArrayList<>(valids.size());
        for (int gi = 0; gi < groupIds.size(); gi++) {
            Long poId = poIds.get(gi);
            for (ValidRow v : groups.get(groupIds.get(gi))) {
                lineRows.add(new PoBulkRepository.PoItemRow(
                        poId, v.itemId, v.quantity, v.unitPrice, v.amount, now));
            }
        }
        poBulkRepository.batchInsertPoItems(lineRows);

        return new BulkPoResponse(total, valids.size(), 0, groupIds.size(), errors);
    }

    /**
     * 행 단위로 형식오류 + 업무검증(공급사/품목/수량/단가/납기일/그룹 일관성)을 수행한다. 저장은 하지 않는다.
     */
    private ValidationResult validateAll(List<PoExcelParser.ParsedRow> parsed) {
        // 참조되는 공급사/품목을 한 번에 로딩
        java.util.Set<String> vendorCodes = new java.util.HashSet<>();
        java.util.Set<String> itemCodes = new java.util.HashSet<>();
        for (PoExcelParser.ParsedRow p : parsed) {
            if (p.row.getVendorCode() != null) vendorCodes.add(p.row.getVendorCode());
            if (p.row.getItemCode() != null) itemCodes.add(p.row.getItemCode());
        }
        Map<String, Vendor> vendorMap = new HashMap<>();
        if (!vendorCodes.isEmpty()) {
            for (Vendor v : vendorRepository.findByVendorCodeIn(vendorCodes)) {
                vendorMap.put(v.getVendorCode(), v);
            }
        }
        Map<String, Item> itemMap = new HashMap<>();
        if (!itemCodes.isEmpty()) {
            for (Item it : itemRepository.findByItemCodeIn(itemCodes)) {
                itemMap.put(it.getItemCode(), it);
            }
        }

        Map<String, HeaderInfo> headers = new LinkedHashMap<>();
        List<RowCheck> checks = new ArrayList<>(parsed.size());

        for (PoExcelParser.ParsedRow p : parsed) {
            BulkPoRow r = p.row;

            // 미리보기용 이름 미리 채우기(가능한 경우)
            String vendorName = (r.getVendorCode() != null && vendorMap.containsKey(r.getVendorCode()))
                    ? vendorMap.get(r.getVendorCode()).getVendorName() : null;
            Item item = (r.getItemCode() != null) ? itemMap.get(r.getItemCode()) : null;
            String itemName = (item != null) ? item.getItemName() : null;
            String groupLabel = groupLabel(r);

            // 1) 파서 형식오류
            if (p.parseError != null) {
                checks.add(RowCheck.error(p.rowNo, r, groupLabel, vendorName, itemName, null, null, p.parseError));
                continue;
            }
            // 2) 업무 검증
            if (r.getVendorCode() == null) {
                checks.add(RowCheck.error(p.rowNo, r, groupLabel, vendorName, itemName, null, null, "공급사코드는 필수입니다."));
                continue;
            }
            Vendor vendor = vendorMap.get(r.getVendorCode());
            if (vendor == null) {
                checks.add(RowCheck.error(p.rowNo, r, groupLabel, vendorName, itemName, null, null,
                        "공급사 없음: 공급사코드=" + r.getVendorCode()));
                continue;
            }
            if (r.getItemCode() == null) {
                checks.add(RowCheck.error(p.rowNo, r, groupLabel, vendorName, itemName, null, null, "품목코드는 필수입니다."));
                continue;
            }
            if (item == null) {
                checks.add(RowCheck.error(p.rowNo, r, groupLabel, vendorName, itemName, null, null,
                        "품목 없음: 품목코드=" + r.getItemCode()));
                continue;
            }
            Long qty = r.getQuantity();
            if (qty == null || qty <= 0) {
                checks.add(RowCheck.error(p.rowNo, r, groupLabel, vendorName, itemName, null, null, "수량은 1 이상이어야 합니다."));
                continue;
            }
            if (r.getDeliveryDate() == null) {
                checks.add(RowCheck.error(p.rowNo, r, groupLabel, vendorName, itemName, null, null, "납기일은 필수입니다."));
                continue;
            }
            BigDecimal unitPrice = (r.getUnitPrice() != null) ? r.getUnitPrice() : item.getStandardPrice();
            if (unitPrice == null || unitPrice.signum() < 0) {
                checks.add(RowCheck.error(p.rowNo, r, groupLabel, vendorName, itemName, unitPrice, null, "단가는 0 이상이어야 합니다."));
                continue;
            }
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(qty));

            // 3) 그룹 일관성 검증(같은 발주그룹 내 공급사/납기일이 같아야 함)
            String groupId = groupId(r);
            HeaderInfo head = headers.get(groupId);
            if (head == null) {
                head = new HeaderInfo(r.getVendorCode(), r.getDeliveryDate(), r.getEtc());
                headers.put(groupId, head);
            } else if (!head.vendorCode.equals(r.getVendorCode()) || !head.deliveryDate.equals(r.getDeliveryDate())) {
                checks.add(RowCheck.error(p.rowNo, r, groupLabel, vendorName, itemName, unitPrice, amount,
                        "같은 발주그룹 내 공급사코드/납기일이 다릅니다. (그룹=" + r.getGroupKey() + ")"));
                continue;
            }

            ValidRow valid = new ValidRow(groupId, item.getId(), qty, unitPrice, amount);
            checks.add(RowCheck.ok(p.rowNo, r, groupLabel, vendorName, itemName, unitPrice, amount, valid));
        }
        return new ValidationResult(checks, headers);
    }

    /** groupKey 가 있으면 그것으로, 없으면 (공급사코드|납기일)로 묶는 키 */
    private String groupId(BulkPoRow r) {
        if (r.getGroupKey() != null && !r.getGroupKey().isBlank()) {
            return "G:" + r.getGroupKey().trim();
        }
        return "V:" + r.getVendorCode() + "|" + r.getDeliveryDate();
    }

    private String groupLabel(BulkPoRow r) {
        if (r.getGroupKey() != null && !r.getGroupKey().isBlank()) {
            return "그룹:" + r.getGroupKey().trim();
        }
        String v = (r.getVendorCode() != null) ? r.getVendorCode() : "-";
        String d = (r.getDeliveryDate() != null) ? r.getDeliveryDate().toString() : "-";
        return v + " / " + d;
    }

    /** 발주 헤더 정보(그룹 첫 행 기준) */
    private static class HeaderInfo {
        final String vendorCode;
        final java.time.LocalDate deliveryDate;
        final String etc;

        HeaderInfo(String vendorCode, java.time.LocalDate deliveryDate, String etc) {
            this.vendorCode = vendorCode;
            this.deliveryDate = deliveryDate;
            this.etc = etc;
        }
    }

    /** 검증 통과한 라인(내부용) */
    private static class ValidRow {
        final String groupId;
        final Long itemId;
        final Long quantity;
        final BigDecimal unitPrice;
        final BigDecimal amount;

        ValidRow(String groupId, Long itemId, Long quantity, BigDecimal unitPrice, BigDecimal amount) {
            this.groupId = groupId;
            this.itemId = itemId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.amount = amount;
        }
    }

    /** 행 하나의 검증 판정(내부용) */
    private static class RowCheck {
        final int rowNo;
        final BulkPoRow raw;
        final String groupLabel;
        final String vendorName;
        final String itemName;
        final BigDecimal unitPrice;
        final BigDecimal amount;
        final String error;   // null 이면 정상
        final ValidRow valid; // 정상일 때만 채워짐

        private RowCheck(int rowNo, BulkPoRow raw, String groupLabel, String vendorName, String itemName,
                         BigDecimal unitPrice, BigDecimal amount, String error, ValidRow valid) {
            this.rowNo = rowNo;
            this.raw = raw;
            this.groupLabel = groupLabel;
            this.vendorName = vendorName;
            this.itemName = itemName;
            this.unitPrice = unitPrice;
            this.amount = amount;
            this.error = error;
            this.valid = valid;
        }

        static RowCheck error(int rowNo, BulkPoRow raw, String groupLabel, String vendorName, String itemName,
                              BigDecimal unitPrice, BigDecimal amount, String error) {
            return new RowCheck(rowNo, raw, groupLabel, vendorName, itemName, unitPrice, amount, error, null);
        }

        static RowCheck ok(int rowNo, BulkPoRow raw, String groupLabel, String vendorName, String itemName,
                           BigDecimal unitPrice, BigDecimal amount, ValidRow valid) {
            return new RowCheck(rowNo, raw, groupLabel, vendorName, itemName, unitPrice, amount, null, valid);
        }
    }

    private static class ValidationResult {
        final List<RowCheck> checks;
        final Map<String, HeaderInfo> headers;

        ValidationResult(List<RowCheck> checks, Map<String, HeaderInfo> headers) {
            this.checks = checks;
            this.headers = headers;
        }
    }

}
