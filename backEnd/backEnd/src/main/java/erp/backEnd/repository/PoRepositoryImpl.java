package erp.backEnd.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import erp.backEnd.dto.member.FindMemberResponseDto;
import erp.backEnd.dto.member.QFindMemberResponseDto;
import erp.backEnd.dto.po.*;
import erp.backEnd.entity.*;
import erp.backEnd.enumeration.PoStatus;
import erp.backEnd.repository.support.Querydsl4RepositorySupport;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static erp.backEnd.entity.QItem.item;
import static erp.backEnd.entity.QPo.po;
import static erp.backEnd.entity.QPoItem.poItem;
import static erp.backEnd.entity.QVendor.*;
import static org.springframework.util.StringUtils.isEmpty;

@Repository
public class PoRepositoryImpl extends QuerydslRepositorySupport implements PoRepositoryCustom{

    private JPAQueryFactory queryFactory;

    public PoRepositoryImpl(EntityManager em) {
        super(Po.class);
        this.queryFactory = new JPAQueryFactory(em);
    }

    public List<PoResponse> search() {
        return queryFactory
                .select(new QPoResponse(
                        po.id,
                        vendor.vendorName,
                        vendor.vendorCode,
                        po.deliveryDate,
                        po.poStatus,
                        po.etc,
                        po.createdDate,
                        po.rejectReason
                        )
                )
                .from(po)
                .fetch();
    }

    @Override // count 쿼리 따로해서 최적화
    public Page<PoResponse> searchPageComplex(PoSearchCondition condition, Pageable pageable) {
        List<PoResponse> content = queryFactory
                .select(new QPoResponse(
                        po.id,
                        vendor.vendorName,
                        vendor.vendorCode,
                        po.deliveryDate,
                        po.poStatus,
                        po.etc,
                        po.createdDate,
                        po.rejectReason
                      ))
                .from(po)
                .join(po.vendor, vendor)
                .where(
                        vendorNameEq(condition.getVendorName()),
                        vendorCodeEq(condition.getVendorCode()),
                        poStatusEq(condition.getPoStatus()),
                        deliveryDateEq(condition.getDeliveryDate())
                        )
                .orderBy(po.createdDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        long total = queryFactory
                .select(po)
                .from(po)
                .where(
                        vendorNameEq(condition.getVendorName()),
                        vendorCodeEq(condition.getVendorCode()),
                        poStatusEq(condition.getPoStatus()),
                        deliveryDateEq(condition.getDeliveryDate())
                        )
                .fetchCount();
//         return new PageImpl<>(content, pageable, total);

        JPAQuery<Po> countQuery = queryFactory
                .select(po)
                .from(po)
                .where(
                        vendorNameEq(condition.getVendorName()),
                        vendorCodeEq(condition.getVendorCode()),
                        poStatusEq(condition.getPoStatus()));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
    }

    private BooleanExpression vendorNameEq(String vendorName) {
        return isEmpty(vendorName) ? null : vendor.vendorName.eq(vendorName);
    }
    private BooleanExpression vendorCodeEq(String vendorCode) {
        return isEmpty(vendorCode) ? null : vendor.vendorCode.eq(vendorCode);
    }

    private BooleanExpression poStatusEq(PoStatus poStatus) {
        return isEmpty(poStatus) ? null : po.poStatus.eq(poStatus);
    }

    private BooleanExpression deliveryDateEq(LocalDate deliveryDate) {
        return isEmpty(deliveryDate) ? null : po.deliveryDate.eq(deliveryDate);
    }

    @Override
    public Optional<Po> findDetail(Long id) {

        //헤더 + 라인 + 품목 한 번에 조회

        return Optional.ofNullable(
                queryFactory
                .selectFrom(po)
                .join(po.vendor, vendor).fetchJoin()
                .leftJoin(po.poItems, poItem).fetchJoin()
                .leftJoin(poItem.item, item).fetchJoin()
                .where(po.id.eq(id))
                .fetchOne()
        );

    }


}
