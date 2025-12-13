package erp.backEnd.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import erp.backEnd.entity.QReceiptLine;
import erp.backEnd.entity.Receipt;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReceiptRepositoryImpl extends QuerydslRepositorySupport implements ReceiptRepositoryCustom {

    private JPAQueryFactory queryFactory;

    public ReceiptRepositoryImpl(EntityManager em) {
        super(Receipt.class);
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Map<Long, Long> sumReceivedByPoItem(Long poId) {
        QReceiptLine rl = QReceiptLine.receiptLine;

        // (poItemId, sum(receivedQty)) 조회
        List<Tuple> rows = queryFactory
                .select(rl.poItem.poItemId, rl.receivedQty.sum())
                .from(rl)
                .where(rl.poItem.po.id.eq(poId))
                .groupBy(rl.poItem.poItemId)
                .fetch();

        Map<Long, Long> map = new HashMap<>();
        for (Tuple t : rows) {
            Long poItemId = t.get(rl.poItem.poItemId);
            Long sum = t.get(rl.receivedQty.sum());
            map.put(poItemId, sum == null ? 0L : sum);
        }
        return map;
    }

}
