package erp.backEnd.service;

import erp.backEnd.dto.po.PoCreateRequest;
import erp.backEnd.dto.po.PoItemCreateRequest;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.Po;
import erp.backEnd.entity.Vendor;
import erp.backEnd.enumeration.NotificationType;
import erp.backEnd.enumeration.PoStatus;
import erp.backEnd.event.NotificationEvent;
import erp.backEnd.repository.ItemRepository;
import erp.backEnd.repository.MemberRepository;
import erp.backEnd.repository.PoBulkRepository;
import erp.backEnd.repository.PoItemRepository;
import erp.backEnd.repository.PoRepository;
import erp.backEnd.repository.VendorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PoServiceImpl 단위 테스트(Mockito). 리포지토리/이벤트발행기를 모킹해
 * 발주 저장·승인·반려·삭제·입고진행의 정상/예외 흐름을 검증한다.
 *
 * ※ @Transactional 롤백은 단위 테스트에서 동작하지 않으므로 "예외가 던져지는가 /
 *   어떤 협력 객체가 호출·미호출 되는가"를 검증한다(상태 롤백은 통합 테스트 영역).
 */
@ExtendWith(MockitoExtension.class)
class PoServiceImplTest {

    @Mock private PoRepository poRepository;
    @Mock private PoItemRepository poItemRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private PoExcelParser poExcelParser;      // save/approve 등에는 미사용(생성자 주입용)
    @Mock private PoBulkRepository poBulkRepository; // 미사용(생성자 주입용)
    @Mock private MemberRepository memberRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PoServiceImpl service;

    private final Vendor vendor = Vendor.of("V1", "공급사1");
    private final LocalDate deliveryDate = LocalDate.of(2026, 8, 1);

    private Po po(PoStatus status) {
        return Po.of(vendor, deliveryDate, status, "비고");
    }

    private Item item(Long id) {
        return Item.builder()
                .id(id).itemCode("I1").itemName("품목1")
                .standardPrice(new BigDecimal("1000")).stockQty(100L).version(0L)
                .build();
    }

    private PoCreateRequest saveRequest(String vendorCode, Long itemId) {
        PoCreateRequest req = new PoCreateRequest();
        req.setVendorCode(vendorCode);
        req.setDeliveryDate(deliveryDate);
        req.setEtc("비고");
        PoItemCreateRequest line = new PoItemCreateRequest();
        line.setItemId(itemId);
        line.setQuantity(5L);
        line.setUnitPrice(new BigDecimal("1000"));
        line.setAmount(new BigDecimal("5000"));
        req.setLines(List.of(line));
        return req;
    }

    @Nested
    @DisplayName("save(req): 발주 신규 등록")
    class Save {

        @Test
        @DisplayName("정상이면 DRAFT 로 저장하고 라인 저장 + 관리자 대상 알림을 발행한다")
        void 정상_저장및이벤트발행() {
            // given: 공급사/품목 존재, save 는 전달된 Po 를 그대로 반환
            PoCreateRequest req = saveRequest("V1", 10L);
            given(vendorRepository.findByVendorCode("V1")).willReturn(Optional.of(vendor));
            given(poRepository.save(any(Po.class))).willAnswer(inv -> inv.getArgument(0));
            given(itemRepository.findById(10L)).willReturn(Optional.of(item(10L)));

            // when
            Po saved = service.save(req);

            // then: DRAFT 상태 + 공급사 연결 + 라인 1건 반영
            assertThat(saved.getPoStatus()).isEqualTo(PoStatus.DRAFT);
            assertThat(saved.getVendor()).isEqualTo(vendor);
            assertThat(saved.getPoItems()).hasSize(1);

            // then: 라인 일괄 저장 호출 + 관리자(ADMINS) 대상 PO_REQUESTED 이벤트 발행
            verify(poItemRepository).saveAll(anyList());
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(NotificationType.PO_REQUESTED);
            assertThat(captor.getValue().getTarget()).isEqualTo(NotificationEvent.Target.ADMINS);
        }

        @Test
        @DisplayName("공급사가 없으면 IllegalArgumentException(저장·이벤트 없음)")
        void 공급사없음_예외() {
            // given: 공급사 조회 실패
            PoCreateRequest req = saveRequest("NONE", 10L);
            given(vendorRepository.findByVendorCode("NONE")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.save(req))
                    .isInstanceOf(IllegalArgumentException.class);

            // then: 발주/라인 저장, 알림 모두 없음
            verify(poRepository, never()).save(any());
            verify(poItemRepository, never()).saveAll(anyList());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("라인의 품목이 없으면 IllegalArgumentException(라인 저장·이벤트 없음)")
        void 품목없음_예외() {
            // given: 공급사는 있으나 라인 품목이 존재하지 않음
            PoCreateRequest req = saveRequest("V1", 99L);
            given(vendorRepository.findByVendorCode("V1")).willReturn(Optional.of(vendor));
            given(poRepository.save(any(Po.class))).willAnswer(inv -> inv.getArgument(0));
            given(itemRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.save(req))
                    .isInstanceOf(IllegalArgumentException.class);

            // then: 라인 저장/알림은 발생하지 않음
            verify(poItemRepository, never()).saveAll(anyList());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("approve(id): 발주 승인")
    class Approve {

        @Test
        @DisplayName("DRAFT 면 APPROVED 로 전이 + 신청자 대상 알림 발행")
        void draft_승인성공() {
            // given
            Po po = po(PoStatus.DRAFT);
            given(poRepository.findById(1L)).willReturn(Optional.of(po));

            // when
            service.approve(1L);

            // then: 상태 APPROVED + PO_APPROVED 이벤트(USER 대상) 발행
            assertThat(po.getPoStatus()).isEqualTo(PoStatus.APPROVED);
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(NotificationType.PO_APPROVED);
            assertThat(captor.getValue().getTarget()).isEqualTo(NotificationEvent.Target.USER);
        }

        @Test
        @DisplayName("대상이 없으면 IllegalArgumentException(이벤트 없음)")
        void 대상없음_예외() {
            given(poRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.approve(99L))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("DRAFT 가 아니면 IllegalStateException(이벤트 없음)")
        void draft아님_예외() {
            // given: 이미 승인된 발주
            given(poRepository.findById(1L)).willReturn(Optional.of(po(PoStatus.APPROVED)));

            assertThatThrownBy(() -> service.approve(1L))
                    .isInstanceOf(IllegalStateException.class);
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("reject(id, reason): 발주 반려")
    class Reject {

        @Test
        @DisplayName("DRAFT + 유효 사유면 REJECTED 로 전이 + 사유 저장 + 알림 발행")
        void draft_반려성공() {
            Po po = po(PoStatus.DRAFT);
            given(poRepository.findById(1L)).willReturn(Optional.of(po));

            service.reject(1L, "단가 과다");

            assertThat(po.getPoStatus()).isEqualTo(PoStatus.REJECTED);
            assertThat(po.getRejectReason()).isEqualTo("단가 과다");
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(NotificationType.PO_REJECTED);
        }

        @Test
        @DisplayName("대상이 없으면 IllegalArgumentException")
        void 대상없음_예외() {
            given(poRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.reject(99L, "사유"))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("사유가 공백이면 IllegalArgumentException(엔티티 가드, 이벤트 없음)")
        void 사유공백_예외() {
            given(poRepository.findById(1L)).willReturn(Optional.of(po(PoStatus.DRAFT)));

            assertThatThrownBy(() -> service.reject(1L, "   "))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("DRAFT 가 아니면 IllegalStateException(이벤트 없음)")
        void draft아님_예외() {
            given(poRepository.findById(1L)).willReturn(Optional.of(po(PoStatus.APPROVED)));

            assertThatThrownBy(() -> service.reject(1L, "사유"))
                    .isInstanceOf(IllegalStateException.class);
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("delete(id): 발주 삭제")
    class Delete {

        @Test
        @DisplayName("DRAFT 면 라인 삭제 후 헤더 삭제")
        void draft_삭제성공() {
            Po po = po(PoStatus.DRAFT);
            given(poRepository.findById(1L)).willReturn(Optional.of(po));

            service.delete(1L);

            // then: 라인 먼저 삭제 → 발주 헤더 삭제
            verify(poItemRepository).deleteByPo(po);
            verify(poRepository).delete(po);
        }

        @Test
        @DisplayName("대상이 없으면 IllegalArgumentException")
        void 대상없음_예외() {
            given(poRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("DRAFT 가 아니면 IllegalStateException(삭제 안 함)")
        void draft아님_예외() {
            given(poRepository.findById(1L)).willReturn(Optional.of(po(PoStatus.APPROVED)));

            assertThatThrownBy(() -> service.delete(1L))
                    .isInstanceOf(IllegalStateException.class);
            // then: 어떤 삭제도 수행되지 않음
            verify(poItemRepository, never()).deleteByPo(any());
            verify(poRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("startReceiving(id): 입고 진행 시작")
    class StartReceiving {

        @Test
        @DisplayName("APPROVED 면 ORDERED 로 전이된다")
        void approved_입고진행() {
            Po po = po(PoStatus.APPROVED);
            given(poRepository.findById(1L)).willReturn(Optional.of(po));

            service.startReceiving(1L);

            assertThat(po.getPoStatus()).isEqualTo(PoStatus.ORDERED);
        }

        @Test
        @DisplayName("대상이 없으면 IllegalArgumentException")
        void 대상없음_예외() {
            given(poRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.startReceiving(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("APPROVED 가 아니면 IllegalStateException")
        void approved아님_예외() {
            given(poRepository.findById(1L)).willReturn(Optional.of(po(PoStatus.DRAFT)));

            assertThatThrownBy(() -> service.startReceiving(1L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
