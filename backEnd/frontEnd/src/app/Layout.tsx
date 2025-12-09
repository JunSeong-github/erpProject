import { NavLink, Outlet } from "react-router-dom";

const NAV = [
    { to: "/", label: "홈", exact: true },
    { to: "/c/general", label: "일반" },
    { to: "/c/dev", label: "개발" },
    { to: "/c/life", label: "라이프" },
    { to: "/write", label: "새 글" },
];

export default function Layout() {
    const linkStyle = ({ isActive }: { isActive: boolean }) => ({
        padding: "8px 12px",
        borderRadius: 8,
        textDecoration: "none",
        color: isActive ? "#fff" : "#111",
        background: isActive ? "#2563eb" : "transparent",
        border: "1px solid #e5e7eb",
        fontWeight: 600,
    });

    return (
        <div>
            <header style={{
                position:"sticky", top:0, zIndex:50, background:"#fff",
                borderBottom:"1px solid #eee"
            }}>
                <div style={{
                    maxWidth: 1000, margin:"0 auto", padding:"12px 16px",
                    display:"flex", gap:10, alignItems:"center", flexWrap:"wrap"
                }}>
                    {NAV.map(item => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            end={item.exact}           // "/" 활성화 과도매칭 방지
                            style={linkStyle}
                        >
                            {item.label}
                        </NavLink>
                    ))}
                </div>
            </header>

            <main style={{ maxWidth:1000, margin:"24px auto", padding:"0 16px" }}>
                <Outlet />   {/* 하위 라우트가 여기 렌더링됨 */}
            </main>
        </div>
    );
}
