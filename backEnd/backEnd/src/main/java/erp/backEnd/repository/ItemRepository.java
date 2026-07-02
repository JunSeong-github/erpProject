package erp.backEnd.repository;

import erp.backEnd.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long>, ItemRepositoryCustom {
    boolean existsByItemCode(String itemCode);

    boolean existsByItemName(String itemName);

    Optional<Item> findById(Long id);

    /** 대량 발주 업로드: 품목코드 목록으로 한 번에 조회 */
    List<Item> findByItemCodeIn(Collection<String> itemCodes);

    /** 대량 품목 업로드: 품목이름 목록으로 한 번에 조회(중복 검증용) */
    List<Item> findByItemNameIn(Collection<String> itemNames);
}
