package erp.backEnd.dto.notification;

import erp.backEnd.entity.Notification;

import java.time.LocalDateTime;

/**
 * SSE push payload 이자 REST 목록 조회 응답. 두 경로가 같은 모양을 쓰도록 통일한다.
 */
public record NotificationDto(
        Long id,
        String type,
        String title,
        String message,
        String refType,
        Long refId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType().name(),
                n.getTitle(),
                n.getMessage(),
                n.getRefType(),
                n.getRefId(),
                n.isRead(),
                n.getCreatedDate()
        );
    }
}
