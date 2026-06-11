package erp.backEnd.repository;

import erp.backEnd.entity.StockUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockUsageRepository extends JpaRepository<StockUsage, Long>, StockUsageRepositoryCustom {
}
