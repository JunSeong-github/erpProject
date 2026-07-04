import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useNotifications } from "./NotificationContext";
import { sendTestNotification } from "./notificationsApi";

/** 상대 시간 표기: 방금 전 / N분 전 / N시간 전 / N일 전 / 날짜 */
function relTime(ts?: string | null): string {
    if (!ts) return "";
    const t = new Date(ts).getTime();
    if (Number.isNaN(t)) return "";
    const diffSec = Math.floor((Date.now() - t) / 1000);
    if (diffSec < 60) return "방금 전";
    const min = Math.floor(diffSec / 60);
    if (min < 60) return `${min}분 전`;
    const hour = Math.floor(min / 60);
    if (hour < 24) return `${hour}시간 전`;
    const day = Math.floor(hour / 24);
    if (day < 7) return `${day}일 전`;
    return new Date(ts).toLocaleDateString("ko-KR", { month: "2-digit", day: "2-digit" });
}

export default function NotificationBell() {
    const { status, notifications, unreadCount, markAllRead, markRead } = useNotifications();

    // 연결 상태 → 색상/문구
    const conn =
        status === "connected"
            ? { color: "#22c55e", label: "실시간 알림 연결됨", pulse: false }
            : status === "connecting"
              ? { color: "#f59e0b", label: "연결 중…", pulse: true }
              : { color: "#ef4444", label: "재연결 중…", pulse: true };
    const navigate = useNavigate();

    const [open, setOpen] = useState(false);
    const [pulse, setPulse] = useState(false); // 새 알림 도착 애니메이션
    const wrapRef = useRef<HTMLDivElement | null>(null);
    const prevUnread = useRef(unreadCount);

    // 미읽음 수가 "증가"하면(=새 알림 도착) 벨을 흔들고 뱃지를 튕긴다.
    useEffect(() => {
        if (unreadCount > prevUnread.current) {
            setPulse(true);
            const id = setTimeout(() => setPulse(false), 700);
            prevUnread.current = unreadCount;
            return () => clearTimeout(id);
        }
        prevUnread.current = unreadCount;
    }, [unreadCount]);

    // 드롭다운 바깥 클릭 시 닫기
    useEffect(() => {
        if (!open) return;
        const onDown = (e: MouseEvent) => {
            if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) setOpen(false);
        };
        document.addEventListener("mousedown", onDown);
        return () => document.removeEventListener("mousedown", onDown);
    }, [open]);

    // 알림 클릭: 그 항목만 읽음 처리 + (연관 리소스가 있으면) 이동
    const handleItemClick = (n: (typeof notifications)[number]) => {
        if (!n.read) markRead(n.id);
        if (n.refType === "PO" && n.refId) {
            setOpen(false);
            navigate(`/erp/po/${n.refId}`);
        } else if (n.refType === "STOCK_USAGE" && n.refId) {
            setOpen(false);
            navigate(`/erp/stock-usage/${n.refId}`);
        }
        // 이동 대상이 없으면 드롭다운은 열어둔 채 읽음 표시만 갱신
    };

    const handleTest = async () => {
        try {
            await sendTestNotification();
        } catch (e) {
            alert("테스트 알림 전송 실패: " + (e as Error).message);
        }
    };

    return (
        <div ref={wrapRef} style={{ position: "relative" }}>
            {/* 애니메이션 키프레임(인라인 스타일로는 불가하여 1회 주입) */}
            <style>{`
                @keyframes bellShake {
                    0%,100% { transform: rotate(0); }
                    15% { transform: rotate(-16deg); }
                    30% { transform: rotate(13deg); }
                    45% { transform: rotate(-9deg); }
                    60% { transform: rotate(6deg); }
                    75% { transform: rotate(-3deg); }
                }
                @keyframes badgePop {
                    0% { transform: scale(1); }
                    40% { transform: scale(1.45); }
                    100% { transform: scale(1); }
                }
                @keyframes statusBlink {
                    0%,100% { opacity: 1; }
                    50% { opacity: 0.25; }
                }
            `}</style>

            <button
                type="button"
                onClick={() => setOpen((v) => !v)}
                title={conn.label}
                style={{
                    position: "relative",
                    width: 38,
                    height: 38,
                    borderRadius: 10,
                    border: "1px solid #e2e8f0",
                    background: open ? "#eef2ff" : "#fff",
                    cursor: "pointer",
                    fontSize: 18,
                    lineHeight: 1,
                }}
            >
                <span
                    style={{
                        display: "inline-block",
                        transformOrigin: "50% 0",
                        animation: pulse ? "bellShake .7s ease" : "none",
                    }}
                >
                    🔔
                </span>

                {/* 연결 상태 점 (초록=연결, 주황=연결중, 빨강 점멸=재연결) */}
                <span
                    style={{
                        position: "absolute",
                        left: 6,
                        bottom: 6,
                        width: 8,
                        height: 8,
                        borderRadius: 999,
                        background: conn.color,
                        border: "1px solid #fff",
                        animation: conn.pulse ? "statusBlink 1s ease-in-out infinite" : "none",
                    }}
                />

                {/* 안읽음 개수 뱃지 (새 알림 도착 시 튕김) */}
                {unreadCount > 0 && (
                    <span
                        style={{
                            position: "absolute",
                            top: -4,
                            right: -4,
                            minWidth: 18,
                            height: 18,
                            padding: "0 5px",
                            borderRadius: 999,
                            background: "#ef4444",
                            color: "#fff",
                            fontSize: 11,
                            fontWeight: 700,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            animation: pulse ? "badgePop .5s ease" : "none",
                        }}
                    >
                        {unreadCount > 99 ? "99+" : unreadCount}
                    </span>
                )}
            </button>

            {open && (
                <div
                    style={{
                        position: "absolute",
                        right: 0,
                        top: 46,
                        width: 340,
                        maxHeight: 440,
                        overflowY: "auto",
                        background: "#fff",
                        border: "1px solid #e2e8f0",
                        borderRadius: 12,
                        boxShadow: "0 12px 32px rgba(15,23,42,0.14)",
                        zIndex: 50,
                    }}
                >
                    {/* 헤더 */}
                    <div
                        style={{
                            position: "sticky",
                            top: 0,
                            background: "#fff",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "space-between",
                            padding: "12px 14px",
                            borderBottom: "1px solid #f1f5f9",
                        }}
                    >
                        <span style={{ display: "flex", alignItems: "center", gap: 7 }}>
                            <b style={{ fontSize: 14, color: "#0f172a" }}>알림</b>
                            {unreadCount > 0 && (
                                <span style={{ fontSize: 12, color: "#ef4444", fontWeight: 700 }}>
                                    {unreadCount}
                                </span>
                            )}
                        </span>
                        <div style={{ display: "flex", gap: 6 }}>
                            <button
                                type="button"
                                onClick={markAllRead}
                                disabled={unreadCount === 0}
                                style={{
                                    fontSize: 12,
                                    fontWeight: 600,
                                    padding: "4px 8px",
                                    borderRadius: 7,
                                    border: "1px solid #e2e8f0",
                                    background: "#fff",
                                    color: unreadCount === 0 ? "#cbd5e1" : "#4f46e5",
                                    cursor: unreadCount === 0 ? "default" : "pointer",
                                }}
                            >
                                모두 읽음
                            </button>
                            <button
                                type="button"
                                onClick={handleTest}
                                title="나에게 테스트 알림 보내기"
                                style={{
                                    fontSize: 12,
                                    fontWeight: 600,
                                    padding: "4px 8px",
                                    borderRadius: 7,
                                    border: "1px solid #c7d2fe",
                                    background: "#eef2ff",
                                    color: "#4f46e5",
                                    cursor: "pointer",
                                }}
                            >
                                테스트
                            </button>
                        </div>
                    </div>

                    {/* 연결 끊김/재연결 배너 */}
                    {status !== "connected" && (
                        <div
                            style={{
                                display: "flex",
                                alignItems: "center",
                                gap: 8,
                                padding: "8px 14px",
                                fontSize: 12,
                                color: "#92400e",
                                background: "#fffbeb",
                                borderBottom: "1px solid #fde68a",
                            }}
                        >
                            <span
                                style={{
                                    width: 8,
                                    height: 8,
                                    borderRadius: 999,
                                    background: conn.color,
                                    flex: "0 0 auto",
                                    animation: "statusBlink 1s ease-in-out infinite",
                                }}
                            />
                            {conn.label} 복구되면 알림이 자동 동기화됩니다.
                        </div>
                    )}

                    {/* 목록 */}
                    {notifications.length === 0 ? (
                        <div style={{ padding: "32px 14px", textAlign: "center", color: "#94a3b8", fontSize: 13 }}>
                            받은 알림이 없습니다.
                        </div>
                    ) : (
                        notifications.map((n) => {
                            // 이동 가능하거나 아직 안읽음이면 클릭에 의미가 있음
                            const clickable = !!(n.refType && n.refId) || !n.read;
                            return (
                                <div
                                    key={n.id}
                                    onClick={() => handleItemClick(n)}
                                    style={{
                                        padding: "11px 14px",
                                        borderBottom: "1px solid #f8fafc",
                                        display: "flex",
                                        flexDirection: "column",
                                        gap: 3,
                                        cursor: clickable ? "pointer" : "default",
                                        background: n.read ? "transparent" : "#f5f8ff",
                                    }}
                                >
                                    <div
                                        style={{
                                            display: "flex",
                                            justifyContent: "space-between",
                                            gap: 8,
                                            alignItems: "center",
                                        }}
                                    >
                                        <span style={{ display: "flex", alignItems: "center", gap: 6, minWidth: 0 }}>
                                            {!n.read && (
                                                <span
                                                    style={{
                                                        width: 7,
                                                        height: 7,
                                                        borderRadius: 999,
                                                        background: "#3b82f6",
                                                        flex: "0 0 auto",
                                                    }}
                                                />
                                            )}
                                            <b
                                                style={{
                                                    fontSize: 13,
                                                    color: "#0f172a",
                                                    whiteSpace: "nowrap",
                                                    overflow: "hidden",
                                                    textOverflow: "ellipsis",
                                                }}
                                            >
                                                {n.title}
                                            </b>
                                        </span>
                                        <span style={{ fontSize: 11, color: "#94a3b8", flex: "0 0 auto" }}>
                                            {relTime(n.createdAt)}
                                        </span>
                                    </div>
                                    <span style={{ fontSize: 12.5, color: "#475569", lineHeight: 1.45 }}>
                                        {n.message}
                                    </span>
                                </div>
                            );
                        })
                    )}
                </div>
            )}
        </div>
    );
}
