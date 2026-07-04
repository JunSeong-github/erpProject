package erp.backEnd.event;

import erp.backEnd.enumeration.NotificationType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 비즈니스 서비스가 발행하는 알림 도메인 이벤트.
 * 트랜잭션 커밋 후({@code @TransactionalEventListener(AFTER_COMMIT)}) 저장·전송된다.
 *
 * <p>대상은 두 가지:
 * <ul>
 *   <li>{@code ADMINS}: 모든 관리자에게(발주/재고사용 신청 접수). {@code excludeLoginId} 는 제외(보통 신청자 본인).</li>
 *   <li>{@code USER}: 특정 사용자에게(승인/반려 → 신청자). {@code receiverLoginId} 로 지정.</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public class NotificationEvent {

    public enum Target { ADMINS, USER }

    private final Target target;
    private final String receiverLoginId; // USER 대상일 때
    private final String excludeLoginId;  // ADMINS 대상일 때 제외할 로그인(행위자)
    private final NotificationType type;
    private final String title;
    private final String message;
    private final String refType;
    private final Long refId;

    public static NotificationEvent toAdmins(String excludeLoginId, NotificationType type,
                                             String title, String message, String refType, Long refId) {
        return new NotificationEvent(Target.ADMINS, null, excludeLoginId, type, title, message, refType, refId);
    }

    public static NotificationEvent toUser(String receiverLoginId, NotificationType type,
                                           String title, String message, String refType, Long refId) {
        return new NotificationEvent(Target.USER, receiverLoginId, null, type, title, message, refType, refId);
    }
}
