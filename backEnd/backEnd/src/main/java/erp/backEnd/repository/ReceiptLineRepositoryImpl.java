package erp.backEnd.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import erp.backEnd.entity.Po;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

public class ReceiptLineRepositoryImpl extends QuerydslRepositorySupport implements ReceiptLineRepositoryCustom {
    private JPAQueryFactory queryFactory;

    // 생성자 무조건 생성해줘야하고 두개하면 안되고 한개만 해야함
    public ReceiptLineRepositoryImpl(EntityManager em) {
        super(Po.class);
        this.queryFactory = new JPAQueryFactory(em);
    }
}
