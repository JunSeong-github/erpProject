package erp.backEnd.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import erp.backEnd.dto.member.FindMemberResponseDto;
import erp.backEnd.dto.member.QFindMemberResponseDto;
import erp.backEnd.dto.po.PoResponse;
import erp.backEnd.dto.po.PoSearchCondition;
import erp.backEnd.dto.po.QPoResponse;
import erp.backEnd.entity.Po;
import erp.backEnd.entity.QPo;
import erp.backEnd.entity.QVendor;
import erp.backEnd.enumeration.PoStatus;
import erp.backEnd.repository.support.Querydsl4RepositorySupport;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static erp.backEnd.entity.QPo.po;
import static erp.backEnd.entity.QVendor.*;
import static org.springframework.util.StringUtils.isEmpty;

@Repository
public class PoRepositoryImpl extends QuerydslRepositorySupport implements PoRepositoryCustom{

    private JPAQueryFactory queryFactory;

    // 생성자 무조건 생성해줘야하고 두개하면 안되고 한개만 해야함
    public PoRepositoryImpl(EntityManager em) {
        super(Po.class);
        this.queryFactory = new JPAQueryFactory(em);
    }

    public List<PoResponse> search() {
        return queryFactory
                .select(new QPoResponse(
                        po.id,
                        vendor.vendorCode,
                        vendor.vendorName,
                        po.deliveryDate,
                        po.poStatus,
                        po.etc
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
                        vendor.vendorCode,
                        vendor.vendorName,
                        po.deliveryDate,
                        po.poStatus,
                        po.etc
                      ))
                .from(po)
                .where(
                        vendorNameEq(condition.getVendorName()),
                        vendorCodeEq(condition.getVendorCode()),
                        poStatusEq(condition.getPoStatus()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        long total = queryFactory
                .select(po)
                .from(po)
                .where(
                        vendorNameEq(condition.getVendorName()),
                        vendorCodeEq(condition.getVendorCode()),
                        poStatusEq(condition.getPoStatus()))
                .fetchCount();
//         return new PageImpl<>(content, pageable, total);

        // getPage에서 토탈컨텐츠 그냥 계산 되면 함수자체 호출 안하는게있음
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





}
