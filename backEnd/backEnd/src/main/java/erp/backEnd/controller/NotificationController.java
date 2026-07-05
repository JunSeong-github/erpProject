package erp.backEnd.controller;

import erp.backEnd.dto.notification.NotificationDto;
import erp.backEnd.exception.BusinessException;
import erp.backEnd.exception.ErrorCode;
import erp.backEnd.service.NotificationService;
import erp.backEnd.service.SseEmitterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "알림(Notification)", description = "SSE 실시간 알림 구독 및 알림 목록·읽음 처리 API (로그인 필요)")
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
    @Operation(summary = "SSE 알림 구독", description = "현재 로그인 사용자의 SSE 스트림을 연결한다. 이후 서버가 이 사용자에게 알림을 push 한다.")
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
    @Operation(summary = "테스트 알림 전송", description = "연결 확인용. 현재 사용자에게 테스트 알림을 즉시 push 한다.")
    @PostMapping("/test")
    public ResponseEntity<String> test(
            @Parameter(description = "보낼 메시지(생략 시 기본 문구)") @RequestParam(required = false) String message) {
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
    @Operation(summary = "최근 알림 목록", description = "현재 사용자의 최근 알림(최신 50건)을 반환한다. 재접속 시 벨 아이콘 채우기용.")
    @GetMapping
    public ResponseEntity<List<NotificationDto>> list() {
        return ResponseEntity.ok(notificationService.recent(currentLoginId()));
    }

    /** 현재 사용자의 미읽음 개수(뱃지용). */
    @Operation(summary = "미읽음 알림 개수", description = "현재 사용자의 읽지 않은 알림 개수를 반환한다. 뱃지 표시용.")
    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount() {
        return ResponseEntity.ok(notificationService.unreadCount(currentLoginId()));
    }

    /** 현재 사용자의 알림을 모두 읽음 처리. */
    @Operation(summary = "알림 전체 읽음 처리", description = "현재 사용자의 모든 알림을 읽음 상태로 변경한다.")
    @PostMapping("/read-all")
    public ResponseEntity<Void> readAll() {
        notificationService.markAllRead(currentLoginId());
        return ResponseEntity.ok().build();
    }

    /** 현재 사용자의 알림 1건을 읽음 처리. */
    @Operation(summary = "알림 단건 읽음 처리", description = "지정한 알림 1건을 읽음 상태로 변경한다.")
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> readOne(
            @Parameter(description = "알림 ID", example = "1") @PathVariable Long id) {
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
