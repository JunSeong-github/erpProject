package erp.backEnd.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import erp.backEnd.dto.po.QStockUsageResponse;
import erp.backEnd.dto.po.StockUsageResponse;
import erp.backEnd.dto.po.StockUsageSearchCondition;
import erp.backEnd.entity.QItem;
import erp.backEnd.entity.QStockUsage;
import erp.backEnd.entity.StockUsage;
import erp.backEnd.enumeration.UsageStatus;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.isEmpty;

@Repository
public class StockUsageRepositoryImpl implements StockUsageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QStockUsage stockUsage = QStockUsage.stockUsage;
    private static final QItem item = QItem.item;

    public StockUsageRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<StockUsageResponse> searchPage(StockUsageSearchCondition condition, Pageable pageable) {
        List<StockUsageResponse> content = queryFactory
                .select(projection())
                .from(stockUsage)
                .join(stockUsage.item, item)
                .where(
                        itemNameContains(condition.getItemName()),
                        statusEq(condition.getStatus())
                )
                .orderBy(stockUsage.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<StockUsage> countQuery = queryFactory
                .select(stockUsage)
                .from(stockUsage)
                .join(stockUsage.item, item)
                .where(
                        itemNameContains(condition.getItemName()),
                        statusEq(condition.getStatus())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
    }

    @Override
    public Optional<StockUsageResponse> findDetail(Long id) {
        StockUsageResponse result = queryFactory
                .select(projection())
                .from(stockUsage)
                .join(stockUsage.item, item)
                .where(stockUsage.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    private QStockUsageResponse projection() {
        return new QStockUsageResponse(
                stockUsage.id,
                item.id,
                item.itemCode,
                item.itemName,
                stockUsage.purpose,
                stockUsage.usagePlace,
                stockUsage.usageQty,
                stockUsage.usageDate,
                stockUsage.remark,
                stockUsage.status,
                stockUsage.rejectReason,
                stockUsage.createdDate
        );
    }

    private BooleanExpression itemNameContains(String itemName) {
        return isEmpty(itemName) ? null : item.itemName.contains(itemName);
    }

    private BooleanExpression statusEq(String status) {
        return isEmpty(status) ? null : stockUsage.status.eq(UsageStatus.fromCode(status));
    }
}
