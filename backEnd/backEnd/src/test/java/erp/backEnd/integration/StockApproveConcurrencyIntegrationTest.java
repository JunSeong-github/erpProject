package erp.backEnd.integration;

import erp.backEnd.entity.Item;
import erp.backEnd.entity.StockUsage;
import erp.backEnd.enumeration.UsageStatus;
import erp.backEnd.exception.BusinessException;
import erp.backEnd.exception.ErrorCode;
import erp.backEnd.repository.ItemRepository;
import erp.backEnd.repository.StockUsageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 재고사용 승인의 "실제 트랜잭션" 동작을 H2 로 검증하는 통합 테스트.
 *
 * <p>여기서는 서비스 approve 로직(상태 전이 + 원자적 차감 + 부족 시 예외)을 하나의 트랜잭션으로 재현한다.
 * (@CacheEvict/Redis 의존을 피하려 서비스 빈 대신 TransactionTemplate 로 같은 흐름을 실행)</p>
 *
 * <p>※ 동시성/롤백은 실제 커밋·롤백을 관찰해야 하므로, @DataJpaTest 가 기본으로 거는
 * 테스트 자동 롤백 트랜잭션을 각 테스트에서 {@code NOT_SUPPORTED} 로 비활성화하고,
 * 각 리포지토리 호출이 자기 트랜잭션에서 커밋되게 한 뒤 @AfterEach 에서 수동 정리한다.</p>
 */
@DataJpaTest
@DisplayName("재고 승인 동시성/롤백 - 실제 트랜잭션(H2)")
class StockApproveConcurrencyIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private StockUsageRepository stockUsageRepository;
    @Autowired
    private PlatformTransactionManager txManager;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);
    }

    @AfterEach
    void cleanUp() {
        // 비트랜잭션 테스트라 각 테스트가 커밋한 데이터를 수동 정리(FK 순서: 사용내역 먼저)
        stockUsageRepository.deleteAll();
        itemRepository.deleteAll();
    }

    /** 승인 1건을 한 트랜잭션에서 수행: 상태 전이 후 원자적 차감, 재고 부족이면 예외 → 롤백 */
    private void approveInTx(Long usageId) {
        tx.executeWithoutResult(status -> {
            StockUsage usage = stockUsageRepository.findById(usageId).orElseThrow();
            usage.approve(); // REQUESTED -> APPROVED (dirty)
            int updated = itemRepository.decreaseStock(usage.getItem().getId(), usage.getUsageQty());
            if (updated == 0) {
                // 재고 부족 → 예외로 트랜잭션 전체 롤백(위 상태 전이도 취소됨)
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
            }
        });
    }

    private Item item(String code, long stock) {
        return Item.builder()
                .itemCode(code).itemName("품목")
                .standardPrice(new BigDecimal("1000")).stockQty(stock).version(0L)
                .build();
    }

    private StockUsage usage(Item item, String place, long qty) {
        return StockUsage.create(item, "용도", place, qty, LocalDate.of(2026, 8, 1), "비고");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("재고 부족으로 승인 실패 시 상태 변경이 롤백되어 REQUESTED 유지 + 재고 불변")
    void 재고부족_승인시_롤백된다() {
        // given: 재고 5, 사용요청 6개(REQUESTED) 커밋
        Item item = itemRepository.save(item("I1", 5));
        Long usageId = stockUsageRepository.save(usage(item, "lab", 6)).getId();
        Long itemId = item.getId();

        // when & then: 재고 부족으로 BusinessException → 트랜잭션 롤백
        assertThatThrownBy(() -> approveInTx(usageId))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STOCK_NOT_ENOUGH));

        // then: usage.approve() 로 바뀌었던 상태가 롤백되어 여전히 REQUESTED
        assertThat(stockUsageRepository.findById(usageId).orElseThrow().getStatus())
                .isEqualTo(UsageStatus.REQUESTED);
        // then: 재고도 차감되지 않고 5 그대로
        assertThat(itemRepository.findById(itemId).orElseThrow().getStockQty()).isEqualTo(5L);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("재고 1을 두 건이 동시에 승인 → 한 건만 성공, 재고는 음수가 되지 않고 0")
    void 동시승인시_재고음수_방지() throws Exception {
        // given: 재고 1, 각 1개짜리 사용요청 2건(REQUESTED) 커밋
        Item item = itemRepository.save(item("I1", 1));
        Long itemId = item.getId();
        Long u1 = stockUsageRepository.save(usage(item, "A", 1)).getId();
        Long u2 = stockUsageRepository.save(usage(item, "B", 1)).getId();

        // when: 두 승인을 동시에 실행(같은 순간 출발하도록 게이트 사용)
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        Future<Boolean> f1 = pool.submit(approveTask(u1, startGate));
        Future<Boolean> f2 = pool.submit(approveTask(u2, startGate));
        startGate.countDown();
        boolean r1 = f1.get(10, TimeUnit.SECONDS);
        boolean r2 = f2.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        // then: 정확히 한 건만 승인 성공(원자적 차감 WHERE 조건이 경합을 판정)
        int success = (r1 ? 1 : 0) + (r2 ? 1 : 0);
        assertThat(success).isEqualTo(1);

        // then: 재고는 음수가 아니라 0
        assertThat(itemRepository.findById(itemId).orElseThrow().getStockQty()).isEqualTo(0L);

        // then: APPROVED 상태인 사용내역은 정확히 1건
        long approved = Stream.of(u1, u2)
                .map(id -> stockUsageRepository.findById(id).orElseThrow().getStatus())
                .filter(s -> s == UsageStatus.APPROVED)
                .count();
        assertThat(approved).isEqualTo(1);
    }

    /** 게이트가 열리면 승인 트랜잭션을 실행하고, 재고 부족 예외면 false 를 반환 */
    private Callable<Boolean> approveTask(Long usageId, CountDownLatch gate) {
        return () -> {
            gate.await();
            try {
                approveInTx(usageId);
                return true;  // 승인 성공
            } catch (BusinessException e) {
                return false; // 재고 부족으로 실패(정상적인 경합 결과)
            }
        };
    }
}
