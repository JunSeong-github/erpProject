package erp.backEnd.repository;

import erp.backEnd.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 최신 50건(벨 목록용) */
    List<Notification> findTop50ByReceiverLoginIdOrderByIdDesc(String receiverLoginId);

    /** 미읽음 개수(뱃지용) */
    long countByReceiverLoginIdAndReadFalse(String receiverLoginId);

    /** 해당 사용자의 미읽음을 일괄 읽음 처리 */
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.read = true where n.receiverLoginId = :loginId and n.read = false")
    int markAllRead(@Param("loginId") String loginId);

    /**
     * 단건 읽음 처리. receiverLoginId 조건으로 "본인 알림만" 수정하도록 강제한다
     * (남의 알림 id 를 넣어도 0건 → 아무 것도 바뀌지 않음).
     */
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.read = true where n.id = :id and n.receiverLoginId = :loginId and n.read = false")
    int markReadById(@Param("id") Long id, @Param("loginId") String loginId);
}
