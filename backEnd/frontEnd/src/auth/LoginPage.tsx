import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../app/AuthContext";

const errMsg = (e: any) =>
    e?.response?.headers?.["x-error-detail"] ||
    e?.response?.data?.message ||
    e?.message ||
    "로그인에 실패했습니다.";

export default function LoginPage() {
    const { login } = useAuth();
    const navigate = useNavigate();

    const [loginId, setLoginId] = useState("");
    const [password, setPassword] = useState("");
    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!loginId.trim() || !password) {
            alert("아이디와 비밀번호를 입력해 주세요.");
            return;
        }
        setSubmitting(true);
        try {
            await login(loginId.trim(), password);
            navigate("/erp/po", { replace: true });
        } catch (err) {
            alert(errMsg(err));
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div
            style={{
                minHeight: "100vh",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                background: "#f1f5f9",
            }}
        >
            <form
                onSubmit={handleSubmit}
                style={{
                    width: 340,
                    background: "#fff",
                    border: "1px solid #e2e8f0",
                    borderRadius: 14,
                    boxShadow: "0 1px 3px rgba(15,23,42,0.06)",
                    padding: "28px 26px",
                }}
            >
                <div style={{ display: "flex", alignItems: "center", gap: 9, marginBottom: 20 }}>
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
                        }}
                    >
                        E
                    </span>
                    <span style={{ fontWeight: 800, fontSize: 18, color: "#0f172a" }}>ERP 로그인</span>
                </div>

                <label style={{ display: "block", fontSize: 13, fontWeight: 600, marginBottom: 4 }}>
                    아이디
                </label>
                <input
                    type="text"
                    value={loginId}
                    onChange={(e) => setLoginId(e.target.value)}
                    autoFocus
                    style={{ width: "100%", padding: "8px 10px", marginBottom: 14, boxSizing: "border-box" }}
                />

                <label style={{ display: "block", fontSize: 13, fontWeight: 600, marginBottom: 4 }}>
                    비밀번호
                </label>
                <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    style={{ width: "100%", padding: "8px 10px", marginBottom: 20, boxSizing: "border-box" }}
                />

                <button
                    type="submit"
                    disabled={submitting}
                    style={{
                        width: "100%",
                        padding: "10px",
                        background: "#4f46e5",
                        color: "#fff",
                        border: "none",
                        borderRadius: 8,
                        fontWeight: 700,
                        cursor: "pointer",
                    }}
                >
                    {submitting ? "로그인 중..." : "로그인"}
                </button>

                <div style={{ marginTop: 16, fontSize: 12, color: "#64748b", lineHeight: 1.6 }}>
                    테스트 계정<br />
                    · 관리자: admin / admin1234<br />
                    · 직원: employee / employee1234
                </div>
            </form>
        </div>
    );
}
