package erp.backEnd.repository;

import erp.backEnd.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Integer>, ReceiptRepositoryCustom {
    Optional<Receipt> findTopByPo_IdOrderByIdDesc(Long poId);
}
