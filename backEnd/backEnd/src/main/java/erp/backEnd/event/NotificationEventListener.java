package erp.backEnd.event;

import erp.backEnd.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 비즈니스 트랜잭션이 정상 커밋된 뒤에만 알림을 저장·전송한다.
 * (커밋 전 push 하면 이후 롤백돼도 잘못된 알림이 나가는 사고를 방지)
 */
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationEvent(NotificationEvent event) {
        notificationService.dispatch(event);
    }
}
