package erp.backEnd.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자(loginId)별 SSE 연결(SseEmitter)을 보관·전송·정리하는 인메모리 레지스트리.
 *
 * <p>구조: {@code loginId -> (emitterId -> SseEmitter)}
 * 한 사용자가 여러 탭/기기로 접속하면 emitter 가 여러 개 생기므로, loginId 하나에
 * emitterId 로 구분되는 다중 emitter 를 보관하고 전송 시 전부에 push 한다.</p>
 *
 * <p>※ 인메모리라 단일 인스턴스 전제. 다중 인스턴스로 확장 시에는 Redis Pub/Sub 등
 * 외부 브로커가 필요하다. 오프라인/누락 방지는 별도 Notification DB 영속화로 커버한다.</p>
 */
@Slf4j
@Component
public class SseEmitterRegistry {

    /** 연결 타임아웃(1시간). 만료되면 EventSource 가 자동 재연결한다. */
    private static final long DEFAULT_TIMEOUT_MS = 60L * 60 * 1000;

    private final Map<String, Map<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * 구독(연결) 생성. 새 SseEmitter 를 만들어 레지스트리에 등록하고,
     * 연결 종료/타임아웃/에러 시 자동으로 정리되도록 콜백을 건다.
     */
    public SseEmitter subscribe(String loginId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        String emitterId = UUID.randomUUID().toString();

        emitters.computeIfAbsent(loginId, k -> new ConcurrentHashMap<>()).put(emitterId, emitter);

        // 연결 정상 종료(클라이언트 close/네트워크 종료) → 정리
        emitter.onCompletion(() -> {
            remove(loginId, emitterId);
            log.debug("SSE 종료 정리(completion): loginId={}, emitterId={}", loginId, emitterId);
        });
        // 서버측 타임아웃(DEFAULT_TIMEOUT_MS 경과) → 완료 처리 후 정리.
        // complete() 가 onCompletion 도 유발하지만 remove 는 멱등이라 안전.
        emitter.onTimeout(() -> {
            log.debug("SSE 타임아웃 정리: loginId={}, emitterId={}", loginId, emitterId);
            emitter.complete();
            remove(loginId, emitterId);
        });
        // 전송/네트워크 오류 → 정리
        emitter.onError(e -> {
            remove(loginId, emitterId);
            log.debug("SSE 오류 정리: loginId={}, emitterId={}, err={}", loginId, emitterId, e.toString());
        });

        // 최초 연결 확인용 이벤트: 프론트 onopen 확정 + 프록시 최초 버퍼 flush 유도
        sendToEmitter(loginId, emitterId, emitter, "connected", Map.of("message", "SSE 연결 성공"));

        log.debug("SSE 구독 등록: loginId={}, emitterId={}, 현재연결수={}", loginId, emitterId, connectionCount(loginId));
        return emitter;
    }

    /**
     * 특정 사용자의 모든 연결(탭)로 이벤트 전송. 오프라인(연결 없음)이면 조용히 무시한다.
     * (오프라인 사용자용 알림 보존은 Notification DB 영속화로 처리 → 재접속 시 목록 조회)
     */
    public void sendToUser(String loginId, String eventName, Object data) {
        Map<String, SseEmitter> userEmitters = emitters.get(loginId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            // 미접속 사용자에게 보내려 해도 예외 없이 무시(알림 자체는 DB에 이미 저장됨)
            log.debug("SSE 대상 없음(오프라인): loginId={}", loginId);
            return;
        }
        // 스냅샷 순회: 전송 중 remove 되어도 안전
        userEmitters.forEach((id, em) -> sendToEmitter(loginId, id, em, eventName, data));
    }

    private void sendToEmitter(String loginId, String emitterId, SseEmitter emitter, String eventName, Object data) {
        try {
            doSend(emitter, SseEmitter.event()
                    .id(emitterId)
                    .name(eventName)
                    .data(data));
        } catch (IOException | IllegalStateException e) {
            // 이미 끊긴 연결 → 제거
            log.debug("SSE 전송 실패 → emitter 제거: loginId={}, err={}", loginId, e.toString());
            remove(loginId, emitterId);
        }
    }

    /**
     * 한 emitter 로의 전송을 emitter 단위로 직렬화한다.
     * 하트비트 스케줄러 스레드와 알림 발송 스레드가 같은 emitter 에 동시에 send() 하면
     * SseEmitter 가 스레드-세이프하지 않아 스트림이 깨질 수 있으므로 반드시 동기화한다.
     */
    private void doSend(SseEmitter emitter, SseEmitter.SseEventBuilder event) throws IOException {
        synchronized (emitter) {
            emitter.send(event);
        }
    }

    private void remove(String loginId, String emitterId) {
        Map<String, SseEmitter> userEmitters = emitters.get(loginId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitterId);
        if (userEmitters.isEmpty()) {
            emitters.remove(loginId);
        }
    }

    /**
     * 하트비트 겸 리퍼(reaper): 프록시/로드밸런서가 idle 연결을 끊지 않도록 25초마다 주석(:ping)을
     * 보내고, 그때 전송이 실패하는 "이미 죽었지만 콜백이 안 온" 연결을 즉시 정리한다.
     * (onTimeout/onError/onCompletion 콜백이 놓친 좀비 연결까지 여기서 수거)
     */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        int reaped = 0;
        for (Map.Entry<String, Map<String, SseEmitter>> entry : emitters.entrySet()) {
            String loginId = entry.getKey();
            for (Map.Entry<String, SseEmitter> em : entry.getValue().entrySet()) {
                try {
                    doSend(em.getValue(), SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    remove(loginId, em.getKey());
                    reaped++;
                }
            }
        }
        if (reaped > 0 || log.isTraceEnabled()) {
            log.debug("SSE 하트비트: 정리 {}건, 현재 연결 {}건", reaped, totalConnections());
        }
    }

    /** 해당 사용자의 현재 연결(탭) 수 */
    public int connectionCount(String loginId) {
        Map<String, SseEmitter> userEmitters = emitters.get(loginId);
        return userEmitters == null ? 0 : userEmitters.size();
    }

    /** 전체 활성 연결 수(관측용) */
    public int totalConnections() {
        return emitters.values().stream().mapToInt(Map::size).sum();
    }
}
