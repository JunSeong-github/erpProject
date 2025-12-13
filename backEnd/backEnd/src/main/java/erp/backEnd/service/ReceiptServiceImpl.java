package erp.backEnd.service;

import erp.backEnd.dto.po.ReceiptCreateRequest;
import erp.backEnd.dto.po.ReceiptLineCreateRequest;
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
            throw new IllegalStateException("ORDERED 또는 PARTIAL_RECEIVED 상태만 입고 등록 가능합니다.");
        }

        LocalDate receiptDate = (req.getReceiptDate() != null) ? req.getReceiptDate() : LocalDate.now();
        Receipt receipt = Receipt.create(po, receiptDate, req.getRemark());

        // poItemId -> PoItem 매핑
        Map<Long, PoItem> poItemMap = po.getPoItems().stream()
                .collect(Collectors.toMap(PoItem::getPoItemId, Function.identity()));

        List<ReceiptLineCreateRequest> lines = req.getLines();
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("입고 라인이 비어있습니다.");
        }

        for (ReceiptLineCreateRequest lineReq : lines) {

            // 1) 라인 식별자 검증
            Long poItemId = lineReq.getPoItemId();
            if (poItemId == null) {
                throw new IllegalArgumentException("poItemId는 필수입니다.");
            }

            // 2) PO 라인 존재 검증 (클라가 조작/오류로 다른 라인 보낼 수 있음)
            PoItem poItem = poItemMap.get(poItemId);
            if (poItem == null) {
                throw new IllegalArgumentException("PO 라인 없음: " + poItemId);
            }

            // 3) receivedQty 처리: null은 0으로 간주 (입력 안 한 라인도 있을 수 있음)
            long receivedQty = (lineReq.getReceivedQty() == null) ? 0L : lineReq.getReceivedQty();

            // 4) 수량 정책 검증
            if (receivedQty < 0) {
                throw new IllegalArgumentException("입고수량은 0 이상이어야 합니다. poItemId=" + poItemId);
            }

            // (선택) 초과입고 금지 정책이면 여기서 막아버림
            // long orderedQty = poItem.getQuantity();
            // if (receivedQty > orderedQty) {
            //     throw new IllegalArgumentException("초과 입고는 허용되지 않습니다. poItemId=" + poItemId);
            // }

            // 5) 엔티티 생성/추가
            receipt.addLine(
                    ReceiptLine.create(poItem, receivedQty, lineReq.getLineRemark())
            );
        }

        receiptRepository.save(receipt);

        // 누적 입고수량 집계해서 상태결정
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
}
