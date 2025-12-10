package erp.backEnd.repository;

import erp.backEnd.entity.PoItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoItemRepository extends JpaRepository<PoItem, Long>, PoItemRepositoryCustom {
}
