package erp.backEnd.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
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
                .where(itemNameContains(condition.getItemName()),
                        itemCodeContains(condition.getItemCode()))
                .orderBy(item.createdDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Item> countQuery = queryFactory
                .select(item)
                .from(item)
                .where(itemNameContains(condition.getItemName()),
                        itemCodeContains(condition.getItemCode()));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);

    }

    @Override
    public Page<StockResponse> searchStockPage(ItemSearchCondition condition, Pageable pageable) {
        // 재고는 item.stock_qty 컬럼을 정답으로 사용한다(집계 스캔 없이 컬럼 직접 조회).
        List<StockResponse> content = queryFactory
                .select(item.id, item.itemCode, item.itemName, item.standardPrice, item.stockQty)
                .from(item)
                .where(itemNameContains(condition.getItemName()),
                        itemCodeContains(condition.getItemCode()))
                .orderBy(item.itemName.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream()
                .map(t -> StockResponse.builder()
                        .itemId(t.get(item.id))
                        .itemCode(t.get(item.itemCode))
                        .itemName(t.get(item.itemName))
                        .standardPrice(t.get(item.standardPrice))
                        .stockQty(t.get(item.stockQty))
                        .build())
                .collect(Collectors.toList());

        JPAQuery<Item> countQuery = queryFactory
                .select(item)
                .from(item)
                .where(itemNameContains(condition.getItemName()),
                        itemCodeContains(condition.getItemCode()));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
    }

    @Override
    public Long getAggregatedStock(Long itemId) {
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

    private BooleanExpression itemCodeContains(String itemCode) {
        return isEmpty(itemCode) ? null : item.itemCode.contains(itemCode);
    }
}
