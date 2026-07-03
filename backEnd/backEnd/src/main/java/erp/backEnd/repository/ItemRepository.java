package erp.backEnd.repository;

import erp.backEnd.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 재고 원자적 차감. WHERE stock_qty >= :qty 로 "읽고-검사-차감"을 DB 단일 문장에서 원자적으로 처리한다.
     * 반환값이 0이면 재고 부족(경합 없이 판정됨)을 의미한다. version 도 함께 증가시켜 낙관적 락 버전을 일관되게 유지한다.
     */
    @Modifying
    @Query("update Item i set i.stockQty = i.stockQty - :qty, i.version = i.version + 1 " +
            "where i.id = :id and i.stockQty >= :qty")
    int decreaseStock(@Param("id") Long id, @Param("qty") long qty);

    /** 재고 원자적 증가(입고). version 도 함께 증가시킨다. 반환값 0이면 대상 품목이 없음을 의미한다. */
    @Modifying
    @Query("update Item i set i.stockQty = i.stockQty + :qty, i.version = i.version + 1 where i.id = :id")
    int increaseStock(@Param("id") Long id, @Param("qty") long qty);
}
