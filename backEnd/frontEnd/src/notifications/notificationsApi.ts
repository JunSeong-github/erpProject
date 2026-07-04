import { api } from "../lib/axios";

/** 서버 알림 1건 (SSE payload 와 REST 목록이 동일 모양) */
export type ServerNotification = {
    id: number;
    type: string;
    title: string;
    message: string;
    refType?: string | null;
    refId?: number | null;
    read: boolean;
    createdAt?: string | null;
};

/** 최근 알림 목록(최신 50건) — 로그인/재접속 시 벨 채우기 */
export const fetchNotifications = () =>
    api.get<ServerNotification[]>("/notifications").then((r) => r.data);

/** 모두 읽음 처리 */
export const markAllNotificationsRead = () =>
    api.post("/notifications/read-all").then((r) => r.data);

/** 단건 읽음 처리(본인 알림만) */
export const markNotificationRead = (id: number) =>
    api.post(`/notifications/${id}/read`).then((r) => r.data);

/**
 * [연결 확인용] 테스트 알림을 자기 자신에게 보낸다.
 * axios `api` 인스턴스는 withCredentials:true 라 세션 쿠키가 함께 전송된다.
 */
export const sendTestNotification = (message?: string) =>
    api
        .post("/notifications/test", null, {
            params: message ? { message } : undefined,
        })
        .then((r) => r.data as string);
