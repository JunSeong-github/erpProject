package erp.backEnd.repository;

import erp.backEnd.entity.Po;
import erp.backEnd.entity.PoItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoItemRepository extends JpaRepository<PoItem, Long>, PoItemRepositoryCustom {
    // 특정 PO에 달린 라인 전부 삭제
    void deleteByPo(Po po);

    // 해당 품목이 발주(→입고)에 사용됐는지 여부. 사용된 품목은 식별자(품목코드) 변경을 막는 데 쓴다.
    boolean existsByItem_Id(Long itemId);
}
