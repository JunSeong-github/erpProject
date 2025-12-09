import { createBrowserRouter , Navigate} from "react-router-dom";

import AppLayout from "./AppLayout";

// import InventoryPage from "../features/erp/inventory/InventoryPage";
// import PoListPage from "../erp/po/PoListPage";
import PoCreatePage from "../erp/po/PoCreatePage";

export const router = createBrowserRouter([
    {
        path: "/",
        element: <AppLayout />,         // ← 여기서 헤더가 항상 렌더됨
        children: [
            { path: "/", element: <Navigate to="/erp/inv" replace /> },   // ← 기본 리다이렉트
            // { path: "/erp/inv", element: <InventoryPage /> },
            // { path: "/erp/po", element: <PoListPage /> },
            { path: "/erp/po/new", element: <PoCreatePage /> },
            { path: "/erp/po/:id", element: <PoCreatePage /> },
            { path: "*", element: <Navigate to="/" replace /> },          // 안전망
        ],
    },
]);
