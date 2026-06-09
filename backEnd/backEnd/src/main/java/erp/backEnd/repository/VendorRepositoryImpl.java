package erp.backEnd.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import erp.backEnd.dto.po.QVendorResponse;
import erp.backEnd.dto.po.VendorResponse;
import erp.backEnd.dto.po.VendorSearchCondition;
import erp.backEnd.entity.Vendor;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static erp.backEnd.entity.QVendor.vendor;
import static org.springframework.util.StringUtils.isEmpty;

@Repository
public class VendorRepositoryImpl extends QuerydslRepositorySupport implements VendorRepositoryCustom {

    private JPAQueryFactory queryFactory;

    public VendorRepositoryImpl(EntityManager em) {
        super(Vendor.class);
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<VendorResponse> searchPageComplex(VendorSearchCondition condition, Pageable pageable) {
        List<VendorResponse> content = queryFactory
                .select(new QVendorResponse(
                        vendor.vendorCode,
                        vendor.vendorName
                ))
                .from(vendor)
                .where(
                        vendorCodeContains(condition.getVendorCode()),
                        vendorNameContains(condition.getVendorName())
                )
                .orderBy(vendor.createdDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Vendor> countQuery = queryFactory
                .select(vendor)
                .from(vendor)
                .where(
                        vendorCodeContains(condition.getVendorCode()),
                        vendorNameContains(condition.getVendorName())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
    }

    private BooleanExpression vendorCodeContains(String vendorCode) {
        return isEmpty(vendorCode) ? null : vendor.vendorCode.contains(vendorCode);
    }

    private BooleanExpression vendorNameContains(String vendorName) {
        return isEmpty(vendorName) ? null : vendor.vendorName.contains(vendorName);
    }
}
