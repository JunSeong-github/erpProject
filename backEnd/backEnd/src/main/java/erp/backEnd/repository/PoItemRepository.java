package erp.backEnd.repository;

import erp.backEnd.entity.Po;
import erp.backEnd.entity.PoItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoItemRepository extends JpaRepository<PoItem, Long>, PoItemRepositoryCustom {
    // 특정 PO에 달린 라인 전부 삭제
    void deleteByPo(Po po);
}
