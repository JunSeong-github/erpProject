// src/app/AppLayout.tsx
import { Outlet } from "react-router-dom";
import AppHeader from "../components/AppHeader";

export default function AppLayout() {
    return (
        <div>
            <AppHeader />
            <main style={{ padding: 16 }}>
                <Outlet />
            </main>
        </div>
    );
}
