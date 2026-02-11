package erp.backEnd.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import erp.backEnd.dto.po.ItemResponse;
import erp.backEnd.dto.po.ItemSearchCondition;
import erp.backEnd.dto.po.QItemResponse;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.Po;
import erp.backEnd.entity.QItem;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    private BooleanExpression itemNameContains(String itemName) {
        return isEmpty(itemName) ? null : item.itemName.contains(itemName);
    }
}
