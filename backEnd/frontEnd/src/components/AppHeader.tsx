// src/components/AppHeader.tsx
import { NavLink } from "react-router-dom";

export default function AppHeader() {
    const link = (to: string, label: string) => (
        <NavLink
            to={to}
            style={({ isActive }) => ({
                marginRight: 12,
                textDecoration: isActive ? "underline" : "none",
            })}
        >
            {label}
        </NavLink>
    );

    return (
        <header style={{ padding: "12px 16px", borderBottom: "1px solid #ddd" }}>
            <nav style={{ display: "flex", gap: 12 }}>
                {link("/erp/po", "발주")}
                {link("/erp/po/write", "발주작성")}
                {/*{link("/erp/sales", "주문")}*/}
                {/*{link("/erp/sales/new", "주문작성")}*/}
                {/*{link("/erp/grn/new", "입고")}*/}
                {/*{link("/erp/ship/new", "출고")}*/}
                {/*{link("/erp/inv", "재고")}*/}
            </nav>
        </header>
    );
}
