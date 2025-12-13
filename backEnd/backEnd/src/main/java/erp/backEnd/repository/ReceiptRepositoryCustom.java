package erp.backEnd.repository;

import java.util.Map;

public interface ReceiptRepositoryCustom {

    /**
     * poId 기준으로 poItemId별 누적 입고수량 합계를 조회
     * return: (poItemId, sumReceivedQty)
     */
    Map<Long, Long> sumReceivedByPoItem(Long poId);
}
