// src/components/AppHeader.tsx
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../app/AuthContext";

export default function AppHeader() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = async () => {
        await logout();
        navigate("/login", { replace: true });
    };

    const link = (to: string, label: string, end?: boolean) => (
        <NavLink
            to={to}
            end={end}
            style={({ isActive }) => ({
                padding: "7px 14px",
                borderRadius: 8,
                fontSize: 14,
                fontWeight: 600,
                textDecoration: "none",
                transition: "background .15s ease, color .15s ease",
                color: isActive ? "#4f46e5" : "#475569",
                background: isActive ? "#eef2ff" : "transparent",
            })}
        >
            {label}
        </NavLink>
    );

    return (
        <header
            style={{
                position: "sticky",
                top: 0,
                zIndex: 10,
                display: "flex",
                alignItems: "center",
                gap: 24,
                padding: "12px 24px",
                background: "rgba(255,255,255,0.85)",
                backdropFilter: "blur(8px)",
                borderBottom: "1px solid #e2e8f0",
                boxShadow: "0 1px 2px rgba(15,23,42,0.04)",
            }}
        >
            {/* 브랜드 로고 */}
            <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
                <span
                    style={{
                        display: "inline-flex",
                        alignItems: "center",
                        justifyContent: "center",
                        width: 30,
                        height: 30,
                        borderRadius: 8,
                        background: "#4f46e5",
                        color: "#fff",
                        fontWeight: 800,
                        fontSize: 15,
                        letterSpacing: "-0.03em",
                    }}
                >
                    E
                </span>
                <span style={{ fontWeight: 800, fontSize: 16, color: "#0f172a", letterSpacing: "-0.02em" }}>
                    ERP 발주관리
                </span>
            </div>

            <nav style={{ display: "flex", alignItems: "center", gap: 4 }}>
                {link("/erp/po", "발주", true)}
                {link("/erp/po/new", "발주작성")}
                {link("/erp/item", "품목", true)}
                {link("/erp/item/new", "품목작성")}
                {link("/erp/vendor", "공급사", true)}
                {link("/erp/vendor/new", "공급사작성")}
                {link("/erp/stock", "재고", true)}
                {link("/erp/stock-usage", "재고사용", true)}
            </nav>

            {user && (
                <div style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: 12 }}>
                    <span style={{ fontSize: 13, color: "#475569" }}>
                        <b style={{ color: "#0f172a" }}>{user.username}</b>
                        <span
                            style={{
                                marginLeft: 6,
                                padding: "2px 8px",
                                borderRadius: 999,
                                fontSize: 11,
                                fontWeight: 700,
                                color: user.role === "ADMIN" ? "#4f46e5" : "#0f766e",
                                background: user.role === "ADMIN" ? "#eef2ff" : "#ccfbf1",
                            }}
                        >
                            {user.roleLabel}
                        </span>
                    </span>
                    <button
                        type="button"
                        onClick={handleLogout}
                        style={{
                            padding: "6px 12px",
                            borderRadius: 8,
                            border: "1px solid #e2e8f0",
                            background: "#fff",
                            color: "#475569",
                            fontSize: 13,
                            fontWeight: 600,
                            cursor: "pointer",
                        }}
                    >
                        로그아웃
                    </button>
                </div>
            )}
        </header>
    );
}
