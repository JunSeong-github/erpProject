package erp.backEnd.service;

import erp.backEnd.dto.po.StockUsageCreateRequest;
import erp.backEnd.dto.po.StockUsageResponse;
import erp.backEnd.dto.po.StockUsageSearchCondition;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.StockUsage;
import erp.backEnd.enumeration.NotificationType;
import erp.backEnd.event.NotificationEvent;
import erp.backEnd.exception.BusinessException;
import erp.backEnd.exception.ErrorCode;
import erp.backEnd.repository.ItemRepository;
import erp.backEnd.repository.MemberRepository;
import erp.backEnd.repository.StockUsageRepository;
import erp.backEnd.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockUsageServiceImpl implements StockUsageService {

    private final StockUsageRepository stockUsageRepository;
    private final ItemRepository itemRepository;

    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 신청자 표기: "이름(로그인아이디)". 회원 조회 실패 시 아이디만, null 이면 "알 수 없음". */
    private String requesterLabel(String loginId) {
        if (loginId == null) return "알 수 없음";
        return memberRepository.findByLoginId(loginId)
                .map(m -> m.getUsername() + "(" + loginId + ")")
                .orElse(loginId);
    }

    @Override
    @Transactional
    public StockUsage create(StockUsageCreateRequest req) {
        if (req.getUsageQty() == null || req.getUsageQty() <= 0) {
            throw new IllegalArgumentException("사용량은 1 이상이어야 합니다.");
        }

        Item item = itemRepository.findById(req.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("품목 없음: " + req.getItemId()));

        StockUsage usage = StockUsage.create(
                item,
                req.getPurpose(),
                req.getUsagePlace(),
                req.getUsageQty(),
                req.getUsageDate(),
                req.getRemark()
        );

        StockUsage saved = stockUsageRepository.save(usage);

        // [알림 F] 재고사용 신청 접수 → 관리자 전원(신청자 본인 제외)
        String requester = saved.getCreatedBy();
        eventPublisher.publishEvent(NotificationEvent.toAdmins(
                requester, NotificationType.STOCK_USAGE_REQUESTED,
                "새 재고사용 승인 요청",
                String.format("%s님이 재고사용(#%d, 품목 %s, 수량 %d)을 신청했습니다.",
                        requesterLabel(requester),
                        saved.getId(), item.getItemName(), saved.getUsageQty()),
                "STOCK_USAGE", saved.getId()));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockUsageResponse> findSearchPage(StockUsageSearchCondition condition, Pageable pageable) {
        return stockUsageRepository.searchPage(condition, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public StockUsageResponse getDetail(Long id) {
        return stockUsageRepository.findDetail(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.STOCK_CACHE, allEntries = true)  // 사용 승인으로 재고 차감 → 재고 캐시 무효화
    public void approve(Long id) {
        StockUsage usage = stockUsageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("재고 사용 내역 없음: " + id));

        // 요청 상태만 승인 가능(메모리 상태 전이). 여기서 검증에 실패하면 재고를 건드리지 않는다.
        usage.approve();

        // 승인 시 실재고를 원자적으로 차감한다.
        // UPDATE ... WHERE stock_qty >= qty 로 "읽고-검사-차감"이 DB 단일 문장에서 원자적으로 처리되므로,
        // 동일 품목을 동시에 승인해도 재고가 음수가 되는 check-then-act 경합이 발생하지 않는다.
        int updated = itemRepository.decreaseStock(usage.getItem().getId(), usage.getUsageQty());
        if (updated == 0) {
            log.warn("재고 부족 승인 차단 - itemId={}, 사용요청={}",
                    usage.getItem().getId(), usage.getUsageQty());
            // 예외 → 트랜잭션 롤백 → 위 usage.approve() 상태전이도 함께 취소됨
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }

        // [알림 G] 재고사용 승인 → 신청자(작성자)
        eventPublisher.publishEvent(NotificationEvent.toUser(
                usage.getCreatedBy(), NotificationType.STOCK_USAGE_APPROVED,
                "재고사용 승인됨",
                String.format("재고사용 #%d(품목 %s)이(가) 승인되었습니다.",
                        usage.getId(), usage.getItem().getItemName()),
                "STOCK_USAGE", usage.getId()));
    }

    @Override
    @Transactional
    public void reject(Long id, String reason) {
        StockUsage usage = stockUsageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("재고 사용 내역 없음: " + id));

        usage.reject(reason);

        // [알림 H] 재고사용 반려 → 신청자(작성자)
        eventPublisher.publishEvent(NotificationEvent.toUser(
                usage.getCreatedBy(), NotificationType.STOCK_USAGE_REJECTED,
                "재고사용 반려됨",
                String.format("재고사용 #%d이(가) 반려되었습니다. 사유: %s", usage.getId(), reason),
                "STOCK_USAGE", usage.getId()));
    }
}
