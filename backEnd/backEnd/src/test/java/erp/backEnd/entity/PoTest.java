package erp.backEnd.entity;

import erp.backEnd.dto.po.PoCreateRequest;
import erp.backEnd.enumeration.PoStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Po 엔티티의 상태 전이 규칙을 검증하는 순수 단위 테스트(스프링 컨텍스트/모킹 불필요).
 * Po.of(vendor, deliveryDate, status, etc) 팩토리로 원하는 시작 상태를 만들어 검증한다.
 */
class PoTest {

    // 상태 전이 로직은 vendor/deliveryDate 값에 의존하지 않으므로 공용 고정값 사용
    private final Vendor vendor = Vendor.of("V1", "공급사1");
    private final LocalDate deliveryDate = LocalDate.of(2026, 8, 1);

    private Po po(PoStatus status) {
        return Po.of(vendor, deliveryDate, status, "비고");
    }

    @Nested
    @DisplayName("approve(): 승인")
    class Approve {

        @Test
        @DisplayName("DRAFT 상태면 APPROVED 로 전이된다")
        void draft이면_승인된다() {
            // given: DRAFT(발주요청) 상태의 발주
            Po po = po(PoStatus.DRAFT);

            // when: 승인
            po.approve();

            // then: APPROVED 로 전이
            assertThat(po.getPoStatus()).isEqualTo(PoStatus.APPROVED);
        }

        @Test
        @DisplayName("DRAFT 가 아니면 IllegalStateException 이 발생한다")
        void draft가_아니면_예외() {
            // given: 이미 승인된 발주 (DRAFT 아님)
            Po po = po(PoStatus.APPROVED);

            // when & then: 승인 불가 → 예외
            assertThatThrownBy(po::approve)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DRAFT");
        }
    }

    @Nested
    @DisplayName("reject(reason): 반려")
    class Reject {

        @Test
        @DisplayName("DRAFT 상태면 REJECTED 로 전이되고 사유가 trim 되어 저장된다")
        void draft이면_반려되고_사유저장() {
            // given: DRAFT 상태의 발주
            Po po = po(PoStatus.DRAFT);

            // when: 앞뒤 공백이 포함된 사유로 반려
            po.reject("  단가 과다  ");

            // then: REJECTED 로 전이 + 사유는 앞뒤 공백이 제거되어 저장
            assertThat(po.getPoStatus()).isEqualTo(PoStatus.REJECTED);
            assertThat(po.getRejectReason()).isEqualTo("단가 과다");
        }

        @Test
        @DisplayName("사유가 null 이거나 공백이면 IllegalArgumentException 이 발생하고 상태는 유지된다")
        void 사유가_비면_예외() {
            // given: DRAFT 상태의 발주
            Po po = po(PoStatus.DRAFT);

            // when & then: 공백/ null 사유는 반려 불가
            assertThatThrownBy(() -> po.reject("   "))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> po.reject(null))
                    .isInstanceOf(IllegalArgumentException.class);

            // then: 예외로 전이되지 않아 여전히 DRAFT
            assertThat(po.getPoStatus()).isEqualTo(PoStatus.DRAFT);
        }

        @Test
        @DisplayName("DRAFT 가 아니면 IllegalStateException 이 발생한다")
        void draft가_아니면_예외() {
            // given: 이미 승인된 발주
            Po po = po(PoStatus.APPROVED);

            // when & then: 반려 불가 → 예외
            assertThatThrownBy(() -> po.reject("사유"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("startReceiving(): 입고 진행 시작")
    class StartReceiving {

        @Test
        @DisplayName("APPROVED 상태면 ORDERED 로 전이된다")
        void approved이면_입고진행() {
            // given: 승인된 발주
            Po po = po(PoStatus.APPROVED);

            // when: 입고 진행 시작
            po.startReceiving();

            // then: ORDERED(입고진행) 로 전이
            assertThat(po.getPoStatus()).isEqualTo(PoStatus.ORDERED);
        }

        @Test
        @DisplayName("APPROVED 가 아니면 IllegalStateException 이 발생한다")
        void approved가_아니면_예외() {
            // given: 아직 승인되지 않은(DRAFT) 발주
            Po po = po(PoStatus.DRAFT);

            // when & then: 입고 진행 불가 → 예외
            assertThatThrownBy(po::startReceiving)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("applyReceivingResult(allMatched): 입고 결과 반영")
    class ApplyReceivingResult {

        @Test
        @DisplayName("전량 일치(true)면 RECEIVED 로 전이된다")
        void 전량일치면_전체입고() {
            // given: 입고 진행 중(ORDERED)인 발주
            Po po = po(PoStatus.ORDERED);

            // when: 발주 수량과 입고 수량이 모두 일치
            po.applyReceivingResult(true);

            // then: RECEIVED(전체입고)
            assertThat(po.getPoStatus()).isEqualTo(PoStatus.RECEIVED);
        }

        @Test
        @DisplayName("일부만 일치(false)면 PARTIAL_RECEIVED 로 전이된다")
        void 일부일치면_부분입고() {
            // given: 입고 진행 중(ORDERED)인 발주
            Po po = po(PoStatus.ORDERED);

            // when: 일부 수량만 입고됨
            po.applyReceivingResult(false);

            // then: PARTIAL_RECEIVED(부분입고)
            assertThat(po.getPoStatus()).isEqualTo(PoStatus.PARTIAL_RECEIVED);
        }
    }

    @Nested
    @DisplayName("updateFrom(req, vendor): 발주 수정")
    class UpdateFrom {

        @Test
        @DisplayName("DRAFT 상태면 공급사/납기일/비고가 갱신되고 상태는 DRAFT 로 유지된다")
        void draft이면_수정됨() {
            // given: DRAFT 발주 + 변경할 값(공급사/납기일/비고)
            Po po = po(PoStatus.DRAFT);
            Vendor newVendor = Vendor.of("V2", "새공급사");
            LocalDate newDate = LocalDate.of(2026, 12, 31);
            PoCreateRequest req = new PoCreateRequest("V2", newDate, "새비고", PoStatus.DRAFT);

            // when: 수정 반영
            po.updateFrom(req, newVendor);

            // then: 필드가 갱신되고 상태는 여전히 DRAFT
            assertThat(po.getVendor()).isEqualTo(newVendor);
            assertThat(po.getDeliveryDate()).isEqualTo(newDate);
            assertThat(po.getEtc()).isEqualTo("새비고");
            assertThat(po.getPoStatus()).isEqualTo(PoStatus.DRAFT);
        }

        @Test
        @DisplayName("DRAFT 가 아니면 IllegalStateException 이 발생한다(승인된 발주는 수정 불가)")
        void draft가_아니면_예외() {
            // given: 이미 승인된 발주
            Po po = po(PoStatus.APPROVED);
            PoCreateRequest req = new PoCreateRequest("V2", deliveryDate, "새비고", PoStatus.DRAFT);

            // when & then: 수정 불가 → 예외
            assertThatThrownBy(() -> po.updateFrom(req, vendor))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
