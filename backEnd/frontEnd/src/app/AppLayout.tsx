// src/app/AppLayout.tsx
import { Outlet } from "react-router-dom";
import AppHeader from "../components/AppHeader";

export default function AppLayout() {
    return (
        <div style={{ minHeight: "100%", background: "#f1f5f9" }}>
            <AppHeader />
            <main style={{ maxWidth: 1160, margin: "0 auto", padding: "28px 24px 64px" }}>
                <div
                    style={{
                        background: "#ffffff",
                        border: "1px solid #e2e8f0",
                        borderRadius: 14,
                        boxShadow: "0 1px 3px rgba(15,23,42,0.06)",
                        padding: "24px 26px",
                    }}
                >
                    <Outlet />
                </div>
            </main>
        </div>
    );
}
