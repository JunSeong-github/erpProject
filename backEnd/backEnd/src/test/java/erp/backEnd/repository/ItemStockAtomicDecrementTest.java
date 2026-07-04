package erp.backEnd.repository;

import erp.backEnd.entity.Item;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ItemRepository.decreaseStock 의 "원자적 차감" 을 실제 DB(H2)로 검증하는 통합 테스트.
 *
 * decreaseStock 은 UPDATE ... SET stock_qty = stock_qty - :qty WHERE id = :id AND stock_qty >= :qty
 * 로, "읽고-검사-차감" 을 DB 단일 문장에서 처리한다. 재고가 모자라면 WHERE 가 걸러 0행이 갱신된다.
 *
 * ※ @Modifying 쿼리는 영속성 컨텍스트를 갱신하지 않으므로, 검증 전 em.clear() 후 DB 에서 재조회한다.
 */
@DataJpaTest
@DisplayName("재고 원자적 차감(decreaseStock) - 실제 DB 동작")
class ItemStockAtomicDecrementTest {

    @Autowired
    private ItemRepository itemRepository;

    @PersistenceContext
    private EntityManager em;

    /** 지정 재고를 가진 품목을 저장하고 flush/clear 후 id 반환 */
    private Long newItem(String code, long stock) {
        Item item = Item.builder()
                .itemCode(code)
                .itemName("품목")
                .standardPrice(new BigDecimal("1000"))
                .stockQty(stock)
                .version(0L)
                .build();
        Long id = itemRepository.saveAndFlush(item).getId();
        em.clear();
        return id;
    }

    @Test
    @DisplayName("재고 5에 6 차감 시도 → 0행 반환, 재고는 5로 유지된다")
    void 초과차감은_0행이고_재고변화없음() {
        // given: 재고 5짜리 품목
        Long id = newItem("I1", 5);

        // when: 재고보다 많은 6을 차감 시도
        int updated = itemRepository.decreaseStock(id, 6);

        // then: WHERE stock_qty >= 6 이 걸러 0행 갱신
        assertThat(updated).isZero();

        // then: 실제 DB 재고는 그대로 5 (차감되지 않음)
        em.clear();
        assertThat(itemRepository.findById(id).orElseThrow().getStockQty()).isEqualTo(5L);
    }

    @Test
    @DisplayName("재고 5에 5 차감 → 1행 반환, 재고 0 (경계값은 통과)")
    void 경계차감은_1행이고_재고0() {
        // given: 재고 5짜리 품목
        Long id = newItem("I1", 5);

        // when: 재고와 동일한 5를 차감(경계값)
        int updated = itemRepository.decreaseStock(id, 5);

        // then: 1행 갱신
        assertThat(updated).isEqualTo(1);

        // then: 실제 DB 재고 0
        em.clear();
        assertThat(itemRepository.findById(id).orElseThrow().getStockQty()).isEqualTo(0L);
    }
}
