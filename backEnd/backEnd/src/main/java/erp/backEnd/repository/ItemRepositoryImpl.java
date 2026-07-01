package erp.backEnd.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import erp.backEnd.dto.po.ItemResponse;
import erp.backEnd.dto.po.ItemSearchCondition;
import erp.backEnd.dto.po.QItemResponse;
import erp.backEnd.dto.po.StockResponse;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.Po;
import erp.backEnd.entity.QItem;
import erp.backEnd.entity.QPoItem;
import erp.backEnd.entity.QReceiptLine;
import erp.backEnd.entity.QStockUsage;
import erp.backEnd.enumeration.UsageStatus;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static erp.backEnd.entity.QItem.item;
import static org.springframework.util.StringUtils.isEmpty;

@Repository
public class ItemRepositoryImpl extends QuerydslRepositorySupport implements ItemRepositoryCustom {

    private JPAQueryFactory queryFactory;

    public ItemRepositoryImpl(EntityManager em) {
        super(Item.class);
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<ItemResponse> searchPageComplex(ItemSearchCondition condition, Pageable pageable) {
        List<ItemResponse> content = queryFactory
                .select(new QItemResponse(
                        item.id,
                        item.itemCode,
                        item.itemName,
                        item.standardPrice
                        ))
                .from(item)
                .where(itemNameContains(condition.getItemName()))
                .orderBy(item.createdDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Item> countQuery = queryFactory
                .select(item)
                .from(item)
                .where(itemNameContains(condition.getItemName()));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);

    }

    @Override
    public Page<StockResponse> searchStockPage(ItemSearchCondition condition, Pageable pageable) {
        QReceiptLine receiptLine = QReceiptLine.receiptLine;
        QStockUsage stockUsage = QStockUsage.stockUsage;

        // 1) 현재 페이지에 보여줄 품목만 먼저 조회한다(필터/정렬/페이징).
        //    이후 집계는 이 페이지의 품목 PK 목록으로만 제한한다.
        List<Tuple> itemRows = queryFactory
                .select(item.id, item.itemCode, item.itemName, item.standardPrice)
                .from(item)
                .where(itemNameContains(condition.getItemName()))
                .orderBy(item.itemName.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<Long> itemIds = itemRows.stream()
                .map(t -> t.get(item.id))
                .collect(Collectors.toList());

        // 집계할 품목이 없으면 빈 페이지로 즉시 반환(불필요한 IN () 쿼리 방지)
        if (itemIds.isEmpty()) {
            JPAQuery<Item> emptyCountQuery = queryFactory
                    .select(item)
                    .from(item)
                    .where(itemNameContains(condition.getItemName()));
            return PageableExecutionUtils.getPage(List.of(), pageable, emptyCountQuery::fetchCount);
        }

        // 재사용할 표현식(집계함수/그룹키)을 미리 선언한다.
        NumberPath<Long> receiptItemId = receiptLine.poItem.item.id;
        NumberExpression<Long> receivedSum = receiptLine.receivedQty.sum();
        NumberExpression<Long> usedSum = stockUsage.usageQty.sum();

        // 2) 품목별 누적 입고수량을 GROUP BY로 "한 번에" 사전 집계한다.
        //    (상관 서브쿼리처럼 품목마다 실행되지 않고, 인덱스로 한 번 스캔한다.)
        Map<Long, Long> receivedByItem = queryFactory
                .select(receiptItemId, receivedSum)
                .from(receiptLine)
                .where(receiptItemId.in(itemIds))
                .groupBy(receiptItemId)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> t.get(receiptItemId),
                        t -> {
                            Long v = t.get(receivedSum);
                            return v != null ? v : 0L;
                        }));

        // 3) 품목별 승인(APPROVED)된 사용량을 GROUP BY로 "한 번에" 사전 집계한다.
        Map<Long, Long> usedByItem = queryFactory
                .select(stockUsage.item.id, usedSum)
                .from(stockUsage)
                .where(stockUsage.item.id.in(itemIds)
                        .and(stockUsage.status.eq(UsageStatus.APPROVED)))
                .groupBy(stockUsage.item.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> t.get(stockUsage.item.id),
                        t -> {
                            Long v = t.get(usedSum);
                            return v != null ? v : 0L;
                        }));

        // 4) 메인(품목) 결과와 사전 집계 결과를 품목 PK 기준으로 조인(해시 조인)한다.
        //    재고수량 = 누적 입고수량 - 승인된 사용량 (기존 계산과 동일)
        List<StockResponse> content = itemRows.stream()
                .map(t -> {
                    Long itemId = t.get(item.id);
                    long received = receivedByItem.getOrDefault(itemId, 0L);
                    long used = usedByItem.getOrDefault(itemId, 0L);
                    return StockResponse.builder()
                            .itemId(itemId)
                            .itemCode(t.get(item.itemCode))
                            .itemName(t.get(item.itemName))
                            .standardPrice(t.get(item.standardPrice))
                            .stockQty(received - used)
                            .build();
                })
                .collect(Collectors.toList());

        JPAQuery<Item> countQuery = queryFactory
                .select(item)
                .from(item)
                .where(itemNameContains(condition.getItemName()));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
    }

    @Override
    public Long getCurrentStock(Long itemId) {
        QReceiptLine receiptLine = QReceiptLine.receiptLine;
        QStockUsage stockUsage = QStockUsage.stockUsage;

        Long received = queryFactory
                .select(receiptLine.receivedQty.sum().coalesce(0L))
                .from(receiptLine)
                .where(receiptLine.poItem.item.id.eq(itemId))
                .fetchOne();

        Long used = queryFactory
                .select(stockUsage.usageQty.sum().coalesce(0L))
                .from(stockUsage)
                .where(stockUsage.item.id.eq(itemId)
                        .and(stockUsage.status.eq(UsageStatus.APPROVED)))
                .fetchOne();

        return (received != null ? received : 0L) - (used != null ? used : 0L);
    }

    private BooleanExpression itemNameContains(String itemName) {
        return isEmpty(itemName) ? null : item.itemName.contains(itemName);
    }
}
