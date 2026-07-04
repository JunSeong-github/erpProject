package erp.backEnd.service;

import erp.backEnd.dto.po.StockUsageCreateRequest;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.StockUsage;
import erp.backEnd.enumeration.NotificationType;
import erp.backEnd.enumeration.UsageStatus;
import erp.backEnd.event.NotificationEvent;
import erp.backEnd.exception.BusinessException;
import erp.backEnd.exception.ErrorCode;
import erp.backEnd.repository.ItemRepository;
import erp.backEnd.repository.MemberRepository;
import erp.backEnd.repository.StockUsageRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * StockUsageServiceImpl 단위 테스트(Mockito). 리포지토리/이벤트발행기를 모킹해
 * 재고사용 승인(approve)의 분기와 생성(create)의 유효성 검증을 검증한다.
 *
 * ※ @Transactional 롤백/@CacheEvict 는 프록시가 없는 단위 테스트에서 동작하지 않으므로,
 *   "재고 부족 시 상태 롤백"이나 "동시성 경합" 같은 것은 별도 통합 테스트(@DataJpaTest) 영역이다.
 *   여기서는 "예외가 던져지는가 / 재고차감·이벤트가 호출되는가"를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class StockUsageServiceImplTest {

    @Mock
    private StockUsageRepository stockUsageRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private StockUsageServiceImpl service;

    /** id 를 가진 품목(재고 차감 인자 검증용) */
    private Item item(Long id) {
        return Item.builder()
                .id(id)
                .itemCode("I1")
                .itemName("품목1")
                .standardPrice(new BigDecimal("1000"))
                .stockQty(100L)
                .version(0L)
                .build();
    }

    /** REQUESTED 상태의 재고사용 1건 */
    private StockUsage requestedUsage(Item item, long qty) {
        return StockUsage.create(item, "용도", "사용처", qty, LocalDate.of(2026, 8, 1), "비고");
    }

    private StockUsageCreateRequest createRequest(Long itemId, Long usageQty) {
        StockUsageCreateRequest req = new StockUsageCreateRequest();
        req.setItemId(itemId);
        req.setUsageQty(usageQty);
        req.setPurpose("용도");
        req.setUsagePlace("사용처");
        req.setUsageDate(LocalDate.of(2026, 8, 1));
        req.setRemark("비고");
        return req;
    }

    @Nested
    @DisplayName("approve(id): 재고사용 승인")
    class Approve {

        @Test
        @DisplayName("① 대상이 없으면 IllegalArgumentException, 재고차감·이벤트 없음")
        void 대상없음_예외() {
            // given: 해당 id 의 재고사용이 없음
            given(stockUsageRepository.findById(99L)).willReturn(Optional.empty());

            // when & then: 조회 실패로 예외
            assertThatThrownBy(() -> service.approve(99L))
                    .isInstanceOf(IllegalArgumentException.class);

            // then: 재고 차감/알림 발행은 일어나지 않음
            verify(itemRepository, never()).decreaseStock(anyLong(), anyLong());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("② 상태가 REQUESTED 가 아니면 예외 + decreaseStock 호출 안 됨(verify never)")
        void 요청상태아님_예외_재고차감안함() {
            // given: 이미 승인되어 REQUESTED 가 아닌 재고사용
            Item item = item(10L);
            StockUsage usage = requestedUsage(item, 5L);
            usage.approve(); // → APPROVED 로 만들어 둠
            given(stockUsageRepository.findById(1L)).willReturn(Optional.of(usage));

            // when & then: 엔티티 상태 가드에서 IllegalStateException
            assertThatThrownBy(() -> service.approve(1L))
                    .isInstanceOf(IllegalStateException.class);

            // then: 상태 검증에서 막혀 재고를 건드리지 않고 알림도 없음
            verify(itemRepository, never()).decreaseStock(anyLong(), anyLong());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("③ 재고 충분(decreaseStock=1)면 APPROVED + 알림 발행 + 정확한 인자로 차감")
        void 재고충분_승인_이벤트발행_인자검증() {
            // given: 요청 상태 재고사용(품목 id=10, 수량 5) + 재고 차감 성공(1행)
            Item item = item(10L);
            StockUsage usage = requestedUsage(item, 5L);
            given(stockUsageRepository.findById(1L)).willReturn(Optional.of(usage));
            given(itemRepository.decreaseStock(10L, 5L)).willReturn(1);

            // when: 승인
            service.approve(1L);

            // then: 상태 APPROVED 로 전이
            assertThat(usage.getStatus()).isEqualTo(UsageStatus.APPROVED);

            // then: decreaseStock 이 (품목id=10, 수량=5)로 정확히 1회 호출됨
            verify(itemRepository).decreaseStock(10L, 5L);

            // then: 승인 알림 이벤트가 발행됨(타입 = STOCK_USAGE_APPROVED)
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(NotificationType.STOCK_USAGE_APPROVED);
        }

        @Test
        @DisplayName("④ 재고 부족(decreaseStock=0)면 BusinessException(STOCK_NOT_ENOUGH) + 이벤트 미발행")
        void 재고부족_예외_이벤트미발행() {
            // given: 요청 상태 재고사용 + 재고 차감 실패(0행 = 재고 부족)
            Item item = item(10L);
            StockUsage usage = requestedUsage(item, 5L);
            given(stockUsageRepository.findById(1L)).willReturn(Optional.of(usage));
            given(itemRepository.decreaseStock(10L, 5L)).willReturn(0);

            // when & then: 재고 부족 → 지정된 에러코드의 BusinessException
            assertThatThrownBy(() -> service.approve(1L))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STOCK_NOT_ENOUGH));

            // then: 승인 실패이므로 알림 이벤트는 발행되지 않음
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("create(req): 재고사용 신청 생성 - 유효성 검증")
    class Create {

        @Test
        @DisplayName("사용량이 null 이면 IllegalArgumentException(리포지토리 호출 없음)")
        void 사용량_null_예외() {
            // given: 사용량 null
            StockUsageCreateRequest req = createRequest(1L, null);

            // when & then
            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(IllegalArgumentException.class);

            // then: 검증 단계에서 막혀 어떤 리포지토리/이벤트도 호출되지 않음
            verifyNoInteractions(itemRepository, stockUsageRepository, eventPublisher);
        }

        @Test
        @DisplayName("사용량이 0 이하면 IllegalArgumentException(리포지토리 호출 없음)")
        void 사용량_0이하_예외() {
            // given: 사용량 0
            StockUsageCreateRequest req = createRequest(1L, 0L);

            // when & then
            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(itemRepository, stockUsageRepository, eventPublisher);
        }

        @Test
        @DisplayName("품목이 없으면 IllegalArgumentException(저장·이벤트 없음)")
        void 품목없음_예외() {
            // given: 유효한 수량이지만 품목이 존재하지 않음
            StockUsageCreateRequest req = createRequest(99L, 5L);
            given(itemRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(IllegalArgumentException.class);

            // then: 저장/알림 없음
            verify(stockUsageRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("정상이면 REQUESTED 로 저장되고 관리자 대상 알림 이벤트가 발행된다")
        void 정상_저장및이벤트발행() {
            // given: 유효 요청 + 품목 존재 + save 는 전달된 엔티티를 그대로 반환
            Item item = item(10L);
            StockUsageCreateRequest req = createRequest(10L, 5L);
            given(itemRepository.findById(10L)).willReturn(Optional.of(item));
            given(stockUsageRepository.save(any(StockUsage.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            StockUsage saved = service.create(req);

            // then: REQUESTED 상태로 생성되고 품목이 연결됨
            assertThat(saved.getStatus()).isEqualTo(UsageStatus.REQUESTED);
            assertThat(saved.getItem()).isEqualTo(item);

            // then: 관리자(ADMINS) 대상 STOCK_USAGE_REQUESTED 이벤트 발행
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(NotificationType.STOCK_USAGE_REQUESTED);
            assertThat(captor.getValue().getTarget()).isEqualTo(NotificationEvent.Target.ADMINS);
        }
    }
}
