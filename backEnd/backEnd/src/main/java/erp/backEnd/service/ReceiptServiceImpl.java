package erp.backEnd.service;

import erp.backEnd.dto.po.BulkReceiptPreviewResponse;
import erp.backEnd.dto.po.BulkReceiptRequest;
import erp.backEnd.dto.po.BulkReceiptResponse;
import erp.backEnd.dto.po.BulkReceiptRow;
import erp.backEnd.dto.po.ReceiptCreateRequest;
import erp.backEnd.dto.po.ReceiptLineCreateRequest;
import erp.backEnd.dto.po.ReceiptSummaryResponse;
import erp.backEnd.entity.Po;
import erp.backEnd.entity.PoItem;
import erp.backEnd.entity.Receipt;
import erp.backEnd.entity.ReceiptLine;
import erp.backEnd.enumeration.PoStatus;
import erp.backEnd.exception.BusinessException;
import erp.backEnd.exception.ErrorCode;
import erp.backEnd.repository.PoRepository;
import erp.backEnd.repository.ReceiptBulkRepository;
import erp.backEnd.repository.ReceiptLineRepository;
import erp.backEnd.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private final PoRepository poRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptLineRepository receiptLineRepository;
    private final ReceiptBulkRepository receiptBulkRepository;
    private final ReceiptExcelParser receiptExcelParser;

    @Transactional
    public void createReceipt(Long poId, ReceiptCreateRequest req) {

        Optional<Po> optionalPo = poRepository.findDetail(poId);

        Po po = optionalPo.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (po == null) throw new IllegalArgumentException("PO 없음");

        if (!(po.getPoStatus() == PoStatus.ORDERED || po.getPoStatus() == PoStatus.PARTIAL_RECEIVED)) {
            throw new IllegalStateException("입고진행 또는 부분입고 상태만 입고 등록 가능합니다.");
        }

        LocalDate receiptDate = (req.getReceiptDate() != null) ? req.getReceiptDate() : LocalDate.now();
        Receipt receipt = Receipt.create(po, receiptDate, req.getRemark());

        Map<Long, PoItem> poItemMap = po.getPoItems().stream()
                .collect(Collectors.toMap(PoItem::getPoItemId, Function.identity()));

        List<ReceiptLineCreateRequest> lines = req.getLines();
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("입고 라인이 비어있습니다.");
        }

        for (ReceiptLineCreateRequest lineReq : lines) {

            Long poItemId = lineReq.getPoItemId();
            if (poItemId == null) {
                throw new IllegalArgumentException("poItemId는 필수입니다.");
            }

            PoItem poItem = poItemMap.get(poItemId);
            if (poItem == null) {
                throw new IllegalArgumentException("PO 라인 없음: " + poItemId);
            }

            long receivedQty = (lineReq.getReceivedQty() == null) ? 0L : lineReq.getReceivedQty();
            long totalReceivedQty = (lineReq.getTotalReceivedQty() == null) ? 0L : lineReq.getTotalReceivedQty();

            if (receivedQty < 0) {
                throw new IllegalArgumentException("입고수량은 0 이상이어야 합니다. poItemId=" + poItemId);
            }

//            // 초과입고 금지 -> 앞단에서 진행하는걸로 수정
//             long orderedQty = poItem.getQuantity();
//             if (orderedQty < (receivedQty + totalReceivedQty)) {
//                 throw new IllegalArgumentException("초과 입고는 허용되지 않습니다. poItemId=" + poItemId);
//             }

            receipt.addLine(
                    ReceiptLine.create(poItem, receivedQty, lineReq.getLineRemark())
            );
        }

        receiptRepository.save(receipt);

        Map<Long, Long> receivedSumMap = receiptRepository.sumReceivedByPoItem(poId);

        boolean allMatched = true;
        for (PoItem line : po.getPoItems()) {
            long orderedQty = line.getQuantity();
            long received = receivedSumMap.getOrDefault(line.getPoItemId(), 0L);

            if (received != orderedQty) {
                allMatched = false;
                break;
            }
        }

        po.applyReceivingResult(allMatched);
    }

    @Transactional
    public BulkReceiptResponse bulkCreate(BulkReceiptRequest req) {
        List<BulkReceiptRow> rows = (req == null) ? null : req.getRows();
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("업로드 데이터가 비어있습니다.");
        }
        // JSON 요청은 형식오류가 없으므로 parseError=null, 행번호는 1-based 순번으로 매핑
        List<ReceiptExcelParser.ParsedRow> parsed = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            parsed.add(new ReceiptExcelParser.ParsedRow(i + 1, rows.get(i), null));
        }
        return validateAndSave(parsed);
    }

    @Transactional(readOnly = true)
    public BulkReceiptPreviewResponse bulkPreview(MultipartFile file) {
        List<ReceiptExcelParser.ParsedRow> parsed = receiptExcelParser.parse(file).rows;
        ValidationResult vr = validateAll(parsed);

        BulkReceiptPreviewResponse resp = new BulkReceiptPreviewResponse();
        List<BulkReceiptPreviewResponse.PreviewRow> previewRows = new ArrayList<>(vr.checks.size());
        int errorCount = 0;
        for (RowCheck c : vr.checks) {
            boolean ok = (c.error == null);
            if (!ok) errorCount++;
            BulkReceiptRow raw = c.raw;
            previewRows.add(new BulkReceiptPreviewResponse.PreviewRow(
                    c.rowNo, raw.getPoId(), raw.getPoItemId(), raw.getItemCode(), c.itemName,
                    raw.getReceivedQty(), raw.getReceiptDate(), raw.getRemark(), raw.getLineRemark(),
                    ok, c.error));
        }
        int total = vr.checks.size();
        resp.setTotalRows(total);
        resp.setErrorRows(errorCount);
        resp.setValidRows(total - errorCount);
        resp.setConfirmable(errorCount == 0 && total > 0);
        resp.setRows(previewRows);
        return resp;
    }

    @Transactional
    public BulkReceiptResponse bulkUpload(MultipartFile file) {
        List<ReceiptExcelParser.ParsedRow> parsed = receiptExcelParser.parse(file).rows;
        return validateAndSave(parsed);
    }

    /**
     * 파싱된 행들을 검증하고, 오류가 하나도 없을 때만 batch insert 로 저장한다.
     * 오류가 하나라도 있으면 전체 롤백(아무것도 저장하지 않음)하고 오류 목록만 돌려준다.
     */
    private BulkReceiptResponse validateAndSave(List<ReceiptExcelParser.ParsedRow> parsed) {
        ValidationResult vr = validateAll(parsed);

        // 오류 수집(정책: 하나라도 있으면 전체 롤백)
        List<BulkReceiptResponse.RowError> errors = new ArrayList<>();
        List<ValidRow> valids = new ArrayList<>();
        for (RowCheck c : vr.checks) {
            if (c.error != null) errors.add(new BulkReceiptResponse.RowError(c.rowNo, c.error));
            else valids.add(c.valid);
        }
        int total = vr.checks.size();
        if (!errors.isEmpty()) {
            return new BulkReceiptResponse(total, 0, errors.size(), 0, errors);
        }

        LocalDateTime now = LocalDateTime.now();

        // (poId, receiptDate) 기준으로 입고건(헤더)으로 묶음 (입력 순서 유지)
        LinkedHashMap<String, HeaderGroup> groups = new LinkedHashMap<>();
        for (ValidRow v : valids) {
            String key = v.poId + "|" + v.receiptDate;
            HeaderGroup g = groups.computeIfAbsent(key, k -> new HeaderGroup(v.poId, v.receiptDate, v.remark));
            g.lines.add(v);
        }
        List<HeaderGroup> groupList = new ArrayList<>(groups.values());

        // 헤더 배치 insert → 생성된 receipt_id 회수(입력 순서와 1:1 대응)
        List<ReceiptBulkRepository.ReceiptHeaderRow> headerRows = new ArrayList<>(groupList.size());
        for (HeaderGroup g : groupList) {
            headerRows.add(new ReceiptBulkRepository.ReceiptHeaderRow(g.poId, g.receiptDate, g.remark, now));
        }
        List<Long> receiptIds = receiptBulkRepository.batchInsertReceipts(headerRows);

        // 라인 배치 insert (헤더 PK 연결)
        List<ReceiptBulkRepository.ReceiptLineRow> lineRows = new ArrayList<>(valids.size());
        for (int gi = 0; gi < groupList.size(); gi++) {
            Long receiptId = receiptIds.get(gi);
            for (ValidRow v : groupList.get(gi).lines) {
                lineRows.add(new ReceiptBulkRepository.ReceiptLineRow(
                        receiptId, v.poItemId, v.receivedQty, v.lineRemark, now));
            }
        }
        receiptBulkRepository.batchInsertLines(lineRows);

        // 영향받은 발주들의 상태를 일괄 재계산 (PO별 1회, 누적합 기준이라 중복 계산해도 결과 동일)
        Map<Long, Po> poMap = vr.poMap;
        for (HeaderGroup g : groupList) {
            Po po = poMap.get(g.poId);
            Map<Long, Long> receivedSumMap = receiptRepository.sumReceivedByPoItem(g.poId);
            boolean allMatched = true;
            for (PoItem line : po.getPoItems()) {
                long orderedQty = line.getQuantity();
                long received = receivedSumMap.getOrDefault(line.getPoItemId(), 0L);
                if (received != orderedQty) {
                    allMatched = false;
                    break;
                }
            }
            po.applyReceivingResult(allMatched);
        }

        return new BulkReceiptResponse(total, valids.size(), 0, groupList.size(), errors);
    }

    /**
     * 행 단위로 형식오류(파서) + 업무검증(발주/품목/수량)을 수행한다. 저장은 하지 않는다.
     * 미리보기·확정저장·JSON저장이 모두 이 검증을 공유한다.
     */
    private ValidationResult validateAll(List<ReceiptExcelParser.ParsedRow> parsed) {
        LocalDate today = LocalDate.now();

        // 참조하는 발주(PO)들을 로딩해 검증용 맵 구성(같은 poId 는 한 번만 조회)
        Map<Long, Po> poMap = new HashMap<>();
        Map<Long, PoItem> poItemMap = new HashMap<>(); // poItemId -> PoItem
        for (ReceiptExcelParser.ParsedRow p : parsed) {
            Long poId = p.row.getPoId();
            if (poId == null || poMap.containsKey(poId)) continue;
            poRepository.findDetail(poId).ifPresent(po -> {
                poMap.put(poId, po);
                for (PoItem pi : po.getPoItems()) {
                    poItemMap.put(pi.getPoItemId(), pi);
                }
            });
        }

        // 라인별 기존 누적 입고수량(DB) - 초과입고 검증용
        Map<Long, Long> receivedBase = new HashMap<>();
        for (Long poId : poMap.keySet()) {
            receivedBase.putAll(receiptRepository.sumReceivedByPoItem(poId));
        }
        // 이번 업로드 내에서 같은 라인에 중복 입고되는 수량 누적(같은 라인 여러 행 대비)
        Map<Long, Long> pendingInUpload = new HashMap<>();

        List<RowCheck> checks = new ArrayList<>(parsed.size());
        for (ReceiptExcelParser.ParsedRow p : parsed) {
            BulkReceiptRow r = p.row;

            // 확인 가능한 경우 품목명을 미리 채워 미리보기에 보여준다
            String itemName = null;
            if (r.getPoItemId() != null) {
                PoItem pi = poItemMap.get(r.getPoItemId());
                if (pi != null && pi.getItem() != null) itemName = pi.getItem().getItemName();
            }

            // 1) 파서 단계 형식오류가 있으면 그 사유를 우선 사용(업무검증 생략)
            if (p.parseError != null) {
                checks.add(RowCheck.error(p.rowNo, r, itemName, p.parseError));
                continue;
            }

            // 2) 업무 검증
            if (r.getPoId() == null) {
                checks.add(RowCheck.error(p.rowNo, r, itemName, "발주번호는 필수입니다."));
                continue;
            }
            Po po = poMap.get(r.getPoId());
            if (po == null) {
                checks.add(RowCheck.error(p.rowNo, r, itemName, "발주 없음: 발주번호=" + r.getPoId()));
                continue;
            }
            if (!(po.getPoStatus() == PoStatus.ORDERED || po.getPoStatus() == PoStatus.PARTIAL_RECEIVED)) {
                checks.add(RowCheck.error(p.rowNo, r, itemName,
                        "입고 가능 상태(입고진행/부분입고)가 아님: 발주번호=" + r.getPoId()));
                continue;
            }

            // 발주 라인 식별: 발주라인번호(우선) 또는 품목코드로 해석
            PoItem poItem;
            if (r.getPoItemId() != null) {
                poItem = poItemMap.get(r.getPoItemId());
                if (poItem == null || poItem.getPo() == null || !poItem.getPo().getId().equals(r.getPoId())) {
                    checks.add(RowCheck.error(p.rowNo, r, itemName,
                            "발주 라인 없음/불일치: 발주라인번호=" + r.getPoItemId() + ", 발주번호=" + r.getPoId()));
                    continue;
                }
            } else if (r.getItemCode() != null && !r.getItemCode().isBlank()) {
                // 해당 발주 안에서 품목코드가 일치하는 라인을 찾는다
                String code = r.getItemCode().trim();
                List<PoItem> matches = new ArrayList<>();
                for (PoItem pi : po.getPoItems()) {
                    if (pi.getItem() != null && code.equalsIgnoreCase(pi.getItem().getItemCode())) {
                        matches.add(pi);
                    }
                }
                if (matches.isEmpty()) {
                    checks.add(RowCheck.error(p.rowNo, r, itemName,
                            "발주에 없는 품목코드: " + code + " (발주번호=" + r.getPoId() + ")"));
                    continue;
                }
                if (matches.size() > 1) {
                    checks.add(RowCheck.error(p.rowNo, r, itemName,
                            "품목코드 '" + code + "'가 발주 내 여러 라인에 있습니다. 발주라인번호로 지정해 주세요. (발주번호=" + r.getPoId() + ")"));
                    continue;
                }
                poItem = matches.get(0);
                r.setPoItemId(poItem.getPoItemId()); // 해석 결과를 미리보기/저장에 반영
            } else {
                checks.add(RowCheck.error(p.rowNo, r, itemName, "발주라인번호 또는 품목코드 중 하나는 필수입니다."));
                continue;
            }

            // 확정된 라인 기준으로 품목명 채움(미리보기 표시용)
            if (poItem.getItem() != null) itemName = poItem.getItem().getItemName();

            Long qty = r.getReceivedQty();
            if (qty == null || qty < 0) {
                checks.add(RowCheck.error(p.rowNo, r, itemName, "입고수량은 0 이상이어야 합니다."));
                continue;
            }

            // 초과입고 검증: (기존 누적입고 + 이번 업로드 누적 + 이번 수량) 이 발주수량을 넘으면 오류
            long poItemId = poItem.getPoItemId();
            long ordered = poItem.getQuantity();
            long already = receivedBase.getOrDefault(poItemId, 0L);
            long pending = pendingInUpload.getOrDefault(poItemId, 0L);
            if (already + pending + qty > ordered) {
                checks.add(RowCheck.error(p.rowNo, r, itemName,
                        "누적입고수량 초과: 발주수량=" + ordered + ", 기존입고=" + already
                                + (pending > 0 ? ", 업로드내누적=" + pending : "")
                                + ", 이번=" + qty + " (합계=" + (already + pending + qty) + ")"));
                continue;
            }
            pendingInUpload.put(poItemId, pending + qty);

            LocalDate receiptDate = (r.getReceiptDate() != null) ? r.getReceiptDate() : today;
            ValidRow valid = new ValidRow(r.getPoId(), receiptDate, r.getRemark(),
                    r.getPoItemId(), qty, r.getLineRemark());
            checks.add(RowCheck.ok(p.rowNo, r, itemName, valid));
        }
        return new ValidationResult(checks, poMap);
    }

    /** 검증 결과(행별 판정 + 로딩된 PO 맵) */
    private static class ValidationResult {
        final List<RowCheck> checks;
        final Map<Long, Po> poMap;

        ValidationResult(List<RowCheck> checks, Map<Long, Po> poMap) {
            this.checks = checks;
            this.poMap = poMap;
        }
    }

    /** 행 하나의 검증 판정(내부용) */
    private static class RowCheck {
        final int rowNo;
        final BulkReceiptRow raw;
        final String itemName; // 확인된 품목명(없으면 null)
        final String error;    // null 이면 정상
        final ValidRow valid;  // 정상일 때만 채워짐

        private RowCheck(int rowNo, BulkReceiptRow raw, String itemName, String error, ValidRow valid) {
            this.rowNo = rowNo;
            this.raw = raw;
            this.itemName = itemName;
            this.error = error;
            this.valid = valid;
        }

        static RowCheck error(int rowNo, BulkReceiptRow raw, String itemName, String error) {
            return new RowCheck(rowNo, raw, itemName, error, null);
        }

        static RowCheck ok(int rowNo, BulkReceiptRow raw, String itemName, ValidRow valid) {
            return new RowCheck(rowNo, raw, itemName, null, valid);
        }
    }

    /** 검증 통과한 행(내부용) */
    private static class ValidRow {
        final Long poId;
        final LocalDate receiptDate;
        final String remark;
        final Long poItemId;
        final Long receivedQty;
        final String lineRemark;

        ValidRow(Long poId, LocalDate receiptDate, String remark,
                 Long poItemId, Long receivedQty, String lineRemark) {
            this.poId = poId;
            this.receiptDate = receiptDate;
            this.remark = remark;
            this.poItemId = poItemId;
            this.receivedQty = receivedQty;
            this.lineRemark = lineRemark;
        }
    }

    /** (poId, receiptDate) 로 묶인 입고건(헤더) 그룹(내부용) */
    private static class HeaderGroup {
        final Long poId;
        final LocalDate receiptDate;
        final String remark;
        final List<ValidRow> lines = new ArrayList<>();

        HeaderGroup(Long poId, LocalDate receiptDate, String remark) {
            this.poId = poId;
            this.receiptDate = receiptDate;
            this.remark = remark;
        }
    }

    @Transactional(readOnly = true)
    public ReceiptSummaryResponse getSummary(Long poId) {

        ReceiptSummaryResponse resp = new ReceiptSummaryResponse();

        Map<Long, Long> receivedSumMap = receiptRepository.sumReceivedByPoItem(poId);
        resp.setReceivedQtyMap(receivedSumMap);

        Receipt latest = receiptRepository.findTopByPo_IdOrderByIdDesc(poId).orElse(null);
        if (latest == null) {
            return resp;
        }

        resp.setRemark(latest.getRemark());

        Map<Long, String> lineRemarkMap = new HashMap<>();
        for (ReceiptLine line : latest.getLines()) {
            Long poItemId = line.getPoItem().getPoItemId(); // PoItem PK getter 이름 맞춰줘
            lineRemarkMap.put(poItemId, line.getLineRemark());
        }
        resp.setLineRemarkMap(lineRemarkMap);

        return resp;
    }

}
