package erp.backEnd.repository;

import erp.backEnd.entity.Member;
import erp.backEnd.enumeration.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    Optional<Member> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    /** 관리자 전체(알림 브로드캐스트 대상) */
    List<Member> findByRole(Role role);

}
