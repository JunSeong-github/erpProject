package erp.backEnd.controller;

import erp.backEnd.dto.notification.NotificationDto;
import erp.backEnd.exception.BusinessException;
import erp.backEnd.exception.ErrorCode;
import erp.backEnd.service.NotificationService;
import erp.backEnd.service.SseEmitterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController("notificationController")
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseEmitterRegistry registry;
    private final NotificationService notificationService;

    /**
     * SSE 구독. 로그인한 사용자만 접근 가능(SecurityConfig 에서 authenticated 지정).
     * 현재 세션의 loginId 로 emitter 를 등록하고, 이후 서버가 그 사용자에게 알림을 push 한다.
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(HttpServletResponse response) {
        // 리버스 프록시(Nginx/Render 등)가 SSE 스트림을 버퍼링해 이벤트가 지연/차단되는 것을 방지.
        // (CORS 헤더는 Security 의 CorsFilter 가 별도로 부여하므로 여기서 건드리지 않는다)
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        return registry.subscribe(currentLoginId());
    }

    /**
     * [연결 확인용] 테스트 알림을 자기 자신에게 보낸다.
     * 구독이 정상이면 프론트에서 즉시 알림이 뜬다.
     */
    @PostMapping("/test")
    public ResponseEntity<String> test(@RequestParam(required = false) String message) {
        String loginId = currentLoginId();
        String msg = (message != null && !message.isBlank()) ? message : "테스트 알림입니다 🎉";

        registry.sendToUser(loginId, "notification", Map.of(
                "type", "TEST",
                "message", msg
        ));

        return ResponseEntity.ok(
                "sent to " + loginId + " (connections=" + registry.connectionCount(loginId) + ")");
    }

    /** 현재 사용자의 최근 알림 목록(최신 50건). 재접속 시 벨 아이콘 채우기용. */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> list() {
        return ResponseEntity.ok(notificationService.recent(currentLoginId()));
    }

    /** 현재 사용자의 미읽음 개수(뱃지용). */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount() {
        return ResponseEntity.ok(notificationService.unreadCount(currentLoginId()));
    }

    /** 현재 사용자의 알림을 모두 읽음 처리. */
    @PostMapping("/read-all")
    public ResponseEntity<Void> readAll() {
        notificationService.markAllRead(currentLoginId());
        return ResponseEntity.ok().build();
    }

    /** 현재 사용자의 알림 1건을 읽음 처리. */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> readOne(@PathVariable Long id) {
        notificationService.markRead(id, currentLoginId());
        return ResponseEntity.ok().build();
    }

    /** 현재 세션의 로그인 아이디. 인증이 없으면 401. */
    private String currentLoginId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessException(ErrorCode.NEED_LOGIN);
        }
        return auth.getName();
    }
}
