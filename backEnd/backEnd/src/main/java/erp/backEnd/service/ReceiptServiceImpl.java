package erp.backEnd.service;

import erp.backEnd.dto.po.ReceiptCreateRequest;
import erp.backEnd.dto.po.ReceiptLineCreateRequest;
import erp.backEnd.dto.po.ReceiptSummaryResponse;
import erp.backEnd.entity.Po;
import erp.backEnd.entity.PoItem;
import erp.backEnd.entity.Receipt;
import erp.backEnd.entity.ReceiptLine;
import erp.backEnd.enumeration.PoStatus;
import erp.backEnd.repository.PoRepository;
import erp.backEnd.repository.ReceiptLineRepository;
import erp.backEnd.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private final PoRepository poRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptLineRepository receiptLineRepository;

    @Transactional
    public void createReceipt(Long poId, ReceiptCreateRequest req) {

        Po po = poRepository.findDetail(poId); // vendor + poItems + item fetchJoin 추천
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
