import { createBrowserRouter , Navigate} from "react-router-dom";

import AppLayout from "./AppLayout";

// import InventoryPage from "../features/erp/inventory/InventoryPage";
// import PoListPage from "../erp/po/PoListPage";
import PoCreatePage from "../erp/po/PoCreatePage";
import PoListPage from "../erp/po/PoListPage";
import ReceiptCreatePage from "../erp/receipt/ReceiptCreatePage";


export const router = createBrowserRouter([
    {
        path: "/",
        element: <AppLayout />,
        children: [
            { path: "/", element: <Navigate to="/erp/po" replace /> },   // ← 기본 리다이렉트
            // { path: "/erp/inv", element: <InventoryPage /> },
            { path: "/erp/po", element: <PoListPage /> },
            { path: "/erp/po/new", element: <PoCreatePage /> },
            { path: "/erp/po/:id", element: <PoCreatePage /> },
            { path: "/erp/receipt/:id", element: <ReceiptCreatePage /> },
            { path: "*", element: <Navigate to="/" replace /> },          // 안전망
        ],
    },
]);
