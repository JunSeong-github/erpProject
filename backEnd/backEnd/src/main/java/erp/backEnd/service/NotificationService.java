package erp.backEnd.service;

import erp.backEnd.dto.notification.NotificationDto;
import erp.backEnd.entity.Member;
import erp.backEnd.entity.Notification;
import erp.backEnd.enumeration.Role;
import erp.backEnd.event.NotificationEvent;
import erp.backEnd.repository.MemberRepository;
import erp.backEnd.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 저장(DB) + 실시간 전송(SSE) 창구.
 * 발행/전송 분리를 위해 이벤트 리스너(AFTER_COMMIT)에서 {@link #dispatch}가 호출된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final SseEmitterRegistry sseRegistry;

    /**
     * 비즈니스 트랜잭션이 커밋된 뒤 새 트랜잭션에서 알림을 저장하고 SSE 로 push 한다.
     * (REQUIRES_NEW: AFTER_COMMIT 시점엔 활성 트랜잭션이 없으므로 저장용 트랜잭션을 새로 연다)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(NotificationEvent e) {
        List<String> receivers = resolveReceivers(e);
        for (String loginId : receivers) {
            try {
                Notification saved = notificationRepository.save(Notification.create(
                        loginId, e.getType(), e.getTitle(), e.getMessage(), e.getRefType(), e.getRefId()));
                // 온라인이면 즉시 전달, 오프라인이면 무시(재접속 시 목록 조회로 확인)
                sseRegistry.sendToUser(loginId, "notification", NotificationDto.from(saved));
            } catch (Exception ex) {
                // 알림은 부가기능 → 한 수신자 실패가 나머지/본 업무에 영향 주지 않게 격리
                log.warn("알림 처리 실패: receiver={}, type={}, err={}", loginId, e.getType(), ex.toString());
            }
        }
    }

    private List<String> resolveReceivers(NotificationEvent e) {
        if (e.getTarget() == NotificationEvent.Target.USER) {
            return (e.getReceiverLoginId() == null) ? List.of() : List.of(e.getReceiverLoginId());
        }
        // ADMINS: 관리자 전원(행위자 본인 제외)
        return memberRepository.findByRole(Role.ADMIN).stream()
                .map(Member::getLoginId)
                .filter(id -> !id.equals(e.getExcludeLoginId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> recent(String loginId) {
        return notificationRepository.findTop50ByReceiverLoginIdOrderByIdDesc(loginId)
                .stream().map(NotificationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String loginId) {
        return notificationRepository.countByReceiverLoginIdAndReadFalse(loginId);
    }

    @Transactional
    public void markAllRead(String loginId) {
        notificationRepository.markAllRead(loginId);
    }

    /** 단건 읽음 처리(본인 알림만). */
    @Transactional
    public void markRead(Long id, String loginId) {
        notificationRepository.markReadById(id, loginId);
    }
}
