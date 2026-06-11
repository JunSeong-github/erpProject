package erp.backEnd.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
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

        // 품목별 누적 입고수량
        Expression<Long> receivedExpr = JPAExpressions
                .select(receiptLine.receivedQty.sum().coalesce(0L))
                .from(receiptLine)
                .where(receiptLine.poItem.item.eq(item));

        // 품목별 승인된 사용량 합계
        Expression<Long> usedExpr = JPAExpressions
                .select(stockUsage.usageQty.sum().coalesce(0L))
                .from(stockUsage)
                .where(stockUsage.item.eq(item)
                        .and(stockUsage.status.eq(UsageStatus.APPROVED)));

        List<Tuple> rows = queryFactory
                .select(item.id, item.itemCode, item.itemName, item.standardPrice, receivedExpr, usedExpr)
                .from(item)
                .where(itemNameContains(condition.getItemName()))
                .orderBy(item.itemName.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<StockResponse> content = rows.stream()
                .map(t -> {
                    Long received = t.get(receivedExpr);
                    Long used = t.get(usedExpr);
                    long stockQty = (received != null ? received : 0L) - (used != null ? used : 0L);
                    return StockResponse.builder()
                            .itemId(t.get(item.id))
                            .itemCode(t.get(item.itemCode))
                            .itemName(t.get(item.itemName))
                            .standardPrice(t.get(item.standardPrice))
                            .stockQty(stockQty)
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
