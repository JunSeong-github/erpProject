package erp.backEnd.repository;

import erp.backEnd.entity.Po;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoRepository extends JpaRepository<Po, Long>, PoRepositoryCustom {

}
