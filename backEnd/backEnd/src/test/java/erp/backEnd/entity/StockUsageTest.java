package erp.backEnd.entity;

import erp.backEnd.enumeration.UsageStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * StockUsage 엔티티의 상태 전이 규칙을 검증하는 순수 단위 테스트.
 * 상태를 강제로 세팅하는 setter 가 없으므로, create() → approve()/reject() 흐름으로 상태를 만든다.
 */
class StockUsageTest {

    // 상태 전이 로직은 item 내부값에 의존하지 않으므로 공용 고정값 사용
    private final Item item = Item.of("I1", "품목1", new BigDecimal("1000"));

    /** REQUESTED(요청) 상태의 재고사용 1건 생성 */
    private StockUsage requested() {
        return StockUsage.create(item, "용도", "사용처", 5L, LocalDate.of(2026, 8, 1), "비고");
    }

    @Nested
    @DisplayName("create(): 생성")
    class Create {

        @Test
        @DisplayName("생성 직후 상태는 REQUESTED 이고 입력값이 보존된다")
        void 생성시_REQUESTED() {
            // given & when: 재고사용 신청 생성
            StockUsage usage = StockUsage.create(item, "용도", "사용처", 5L, LocalDate.of(2026, 8, 1), "비고");

            // then: 초기 상태 REQUESTED + 입력값 보존
            assertThat(usage.getStatus()).isEqualTo(UsageStatus.REQUESTED);
            assertThat(usage.getUsageQty()).isEqualTo(5L);
            assertThat(usage.getItem()).isEqualTo(item);
        }
    }

    @Nested
    @DisplayName("approve(): 승인")
    class Approve {

        @Test
        @DisplayName("REQUESTED 상태면 APPROVED 로 전이된다")
        void requested이면_승인() {
            // given: 요청 상태의 재고사용
            StockUsage usage = requested();

            // when: 승인
            usage.approve();

            // then: APPROVED 로 전이
            assertThat(usage.getStatus()).isEqualTo(UsageStatus.APPROVED);
        }

        @Test
        @DisplayName("REQUESTED 가 아니면 IllegalStateException 이 발생한다")
        void requested가_아니면_예외() {
            // given: 이미 승인된 재고사용 (REQUESTED 아님)
            StockUsage usage = requested();
            usage.approve();

            // when & then: 재승인 불가 → 예외
            assertThatThrownBy(usage::approve)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("reject(reason): 반려")
    class Reject {

        @Test
        @DisplayName("REQUESTED 상태면 REJECTED 로 전이되고 사유가 trim 되어 저장된다")
        void requested이면_반려되고_사유저장() {
            // given: 요청 상태의 재고사용
            StockUsage usage = requested();

            // when: 앞뒤 공백이 포함된 사유로 반려
            usage.reject("  재고 부족  ");

            // then: REJECTED 로 전이 + 사유는 trim 되어 저장
            assertThat(usage.getStatus()).isEqualTo(UsageStatus.REJECTED);
            assertThat(usage.getRejectReason()).isEqualTo("재고 부족");
        }

        @Test
        @DisplayName("사유가 null 이거나 공백이면 IllegalArgumentException 이 발생하고 상태는 유지된다")
        void 사유가_비면_예외() {
            // given: 요청 상태의 재고사용
            StockUsage usage = requested();

            // when & then: 공백/ null 사유는 반려 불가
            assertThatThrownBy(() -> usage.reject("   "))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> usage.reject(null))
                    .isInstanceOf(IllegalArgumentException.class);

            // then: 예외로 전이되지 않아 여전히 REQUESTED
            assertThat(usage.getStatus()).isEqualTo(UsageStatus.REQUESTED);
        }

        @Test
        @DisplayName("REQUESTED 가 아니면 IllegalStateException 이 발생한다")
        void requested가_아니면_예외() {
            // given: 이미 승인된 재고사용
            StockUsage usage = requested();
            usage.approve();

            // when & then: 반려 불가 → 예외
            assertThatThrownBy(() -> usage.reject("사유"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
