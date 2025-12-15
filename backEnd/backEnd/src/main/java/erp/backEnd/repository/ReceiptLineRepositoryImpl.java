package erp.backEnd.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import erp.backEnd.entity.Po;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

public class ReceiptLineRepositoryImpl extends QuerydslRepositorySupport implements ReceiptLineRepositoryCustom {
    private JPAQueryFactory queryFactory;

    public ReceiptLineRepositoryImpl(EntityManager em) {
        super(Po.class);
        this.queryFactory = new JPAQueryFactory(em);
    }
}
