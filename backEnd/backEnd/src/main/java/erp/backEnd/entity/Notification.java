package erp.backEnd.entity;

import erp.backEnd.enumeration.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 알림 1건. 온라인이면 SSE 로 즉시 push 되고, 오프라인이면 이 테이블에 남아
 * 재접속 시 벨 아이콘으로 확인된다(놓침 방지).
 */
@Entity
@Table(name = "notification", indexes = {
        // 미읽음 카운트/목록 조회용
        @Index(name = "idx_noti_receiver_read", columnList = "receiver_login_id, is_read"),
        // 최신순 목록 조회용
        @Index(name = "idx_noti_receiver_id", columnList = "receiver_login_id, notification_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", comment = "알림 id")
    private Long id;

    @Column(name = "receiver_login_id", nullable = false, comment = "수신자 로그인아이디")
    private String receiverLoginId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, comment = "알림 유형")
    private NotificationType type;

    @Column(nullable = false, comment = "제목")
    private String title;

    @Column(length = 500, comment = "내용(누가/무엇을)")
    private String message;

    @Column(name = "ref_type", comment = "연관 리소스 유형(PO/STOCK_USAGE)")
    private String refType;

    @Column(name = "ref_id", comment = "연관 리소스 id(클릭 시 이동)")
    private Long refId;

    @Column(name = "is_read", nullable = false, comment = "읽음 여부")
    private boolean read;

    public static Notification create(String receiverLoginId, NotificationType type, String title,
                                      String message, String refType, Long refId) {
        Notification n = new Notification();
        n.receiverLoginId = receiverLoginId;
        n.type = type;
        n.title = title;
        n.message = message;
        n.refType = refType;
        n.refId = refId;
        n.read = false;
        return n;
    }

    public void markRead() {
        this.read = true;
    }
}
