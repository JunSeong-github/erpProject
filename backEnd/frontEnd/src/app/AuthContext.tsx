import { createContext, useContext, useEffect, useState, ReactNode } from "react";
import { AuthUser, fetchMe, login as apiLogin, logout as apiLogout } from "../erp/api";

type AuthContextValue = {
    user: AuthUser | null;
    loading: boolean;
    login: (loginId: string, password: string) => Promise<AuthUser>;
    logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<AuthUser | null>(null);
    const [loading, setLoading] = useState(true);

    // 앱 로드 시 세션 기반 로그인 상태 확인
    useEffect(() => {
        fetchMe()
            .then((u) => setUser(u))
            .catch(() => setUser(null))
            .finally(() => setLoading(false));
    }, []);

    const login = async (loginId: string, password: string) => {
        const u = await apiLogin(loginId, password);
        setUser(u);
        return u;
    };

    const logout = async () => {
        try {
            await apiLogout();
        } finally {
            setUser(null);
        }
    };

    return (
        <AuthContext.Provider value={{ user, loading, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used within AuthProvider");
    return ctx;
}
