package erp.backEnd.repository;

import erp.backEnd.entity.ReceiptLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptLineRepository extends JpaRepository<ReceiptLine, Long>, ReceiptLineRepositoryCustom {
}
