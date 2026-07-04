import {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useRef,
    useState,
    ReactNode,
} from "react";
import { useAuth } from "../app/AuthContext";
import {
    fetchNotifications,
    markAllNotificationsRead,
    markNotificationRead,
    ServerNotification,
} from "./notificationsApi";

const baseUrl = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

export type AppNotification = ServerNotification;

/** SSE 연결 상태 */
export type ConnStatus = "connecting" | "connected" | "reconnecting";

type NotificationContextValue = {
    status: ConnStatus;
    notifications: AppNotification[];
    unreadCount: number;
    markAllRead: () => void;
    markRead: (id: number) => void;
    reload: () => void;
};

const NotificationContext = createContext<NotificationContextValue | undefined>(undefined);

/** 재연결 백오프: 1s → 2s → 4s → 8s → 16s → 최대 30s */
function backoffDelay(attempt: number): number {
    return Math.min(30_000, 1_000 * 2 ** Math.min(attempt - 1, 5));
}

export function NotificationProvider({ children }: { children: ReactNode }) {
    const { user } = useAuth();
    const [status, setStatus] = useState<ConnStatus>("connecting");
    const [notifications, setNotifications] = useState<AppNotification[]>([]);

    const esRef = useRef<EventSource | null>(null);
    const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const attemptsRef = useRef(0);
    const stoppedRef = useRef(false); // 로그아웃/언마운트 후 재연결 방지

    // DB 에서 최근 알림을 다시 불러온다(로그인/재연결 시 → 끊긴 사이 받은 것 포함).
    const reload = useCallback(() => {
        fetchNotifications()
            .then(setNotifications)
            .catch(() => {
                /* 세션 만료 등은 조용히 무시 */
            });
    }, []);

    useEffect(() => {
        // 비로그인: 모든 연결/타이머 정리
        if (!user) {
            stoppedRef.current = true;
            if (timerRef.current) clearTimeout(timerRef.current);
            esRef.current?.close();
            esRef.current = null;
            setNotifications([]);
            setStatus("connecting");
            return;
        }

        stoppedRef.current = false;
        attemptsRef.current = 0;

        // 한 번의 연결 시도. 실패 시 백오프로 직접 재연결한다
        // (브라우저 기본 자동재연결에 맡기지 않고 close() 로 회수 → 지연/중복 제어).
        const connect = () => {
            if (stoppedRef.current) return;

            const es = new EventSource(`${baseUrl}/notifications/subscribe`, {
                withCredentials: true, // 세션 쿠키(JSESSIONID) 전송
            });
            esRef.current = es;

            // 연결 성공(최초/재연결 모두) → 상태 갱신 + 목록 동기화
            es.onopen = () => {
                attemptsRef.current = 0;
                setStatus("connected");
                reload();
            };

            // 실시간 알림 수신 → 목록 맨 앞에 추가(id 중복 방지)
            es.addEventListener("notification", (e) => {
                try {
                    const n = JSON.parse((e as MessageEvent).data) as ServerNotification;
                    setNotifications((prev) =>
                        prev.some((p) => p.id === n.id) ? prev : [n, ...prev].slice(0, 100),
                    );
                } catch {
                    /* 형식이 깨진 이벤트는 무시 */
                }
            });

            // 네트워크 끊김/서버 재시작 등 → 직접 백오프 재연결
            es.onerror = () => {
                es.close();
                esRef.current = null;
                if (stoppedRef.current) return;

                const attempt = ++attemptsRef.current;
                const delay = backoffDelay(attempt);
                setStatus("reconnecting");
                timerRef.current = setTimeout(connect, delay);
            };
        };

        setStatus("connecting");
        connect();

        // 언마운트/사용자 변경 시 정리
        return () => {
            stoppedRef.current = true;
            if (timerRef.current) clearTimeout(timerRef.current);
            esRef.current?.close();
            esRef.current = null;
            setStatus("connecting");
        };
        // 같은 사용자면 재연결하지 않도록 loginId 기준으로 의존
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [user?.loginId]);

    const markAllRead = useCallback(() => {
        setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
        markAllNotificationsRead().catch(() => {
            /* 실패 시 다음 reload 에서 원상복구됨 */
        });
    }, []);

    const markRead = useCallback((id: number) => {
        setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
        markNotificationRead(id).catch(() => {
            /* 실패 시 다음 reload 에서 원상복구됨 */
        });
    }, []);

    const unreadCount = notifications.reduce((acc, n) => (n.read ? acc : acc + 1), 0);

    return (
        <NotificationContext.Provider
            value={{ status, notifications, unreadCount, markAllRead, markRead, reload }}
        >
            {children}
        </NotificationContext.Provider>
    );
}

export function useNotifications() {
    const ctx = useContext(NotificationContext);
    if (!ctx) throw new Error("useNotifications must be used within NotificationProvider");
    return ctx;
}
