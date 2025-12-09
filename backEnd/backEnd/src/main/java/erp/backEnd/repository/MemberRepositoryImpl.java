package erp.backEnd.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import erp.backEnd.dto.member.FindMemberResponseDto;
import erp.backEnd.dto.member.QFindMemberResponseDto;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static erp.backEnd.entity.QMember.member;

@Repository
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    public List<FindMemberResponseDto> search() {
        return queryFactory
                .select(new QFindMemberResponseDto(
                        member.id,
                        member.username,
                        member.age)
                )
                .from(member)
                .fetch();
    }
}
