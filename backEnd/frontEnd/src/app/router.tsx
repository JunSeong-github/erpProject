import { createBrowserRouter , Navigate} from "react-router-dom";

import AppLayout from "./AppLayout";

// import InventoryPage from "../features/erp/inventory/InventoryPage";
// import PoListPage from "../erp/po/PoListPage";
import PoCreatePage from "../erp/po/PoCreatePage";
import PoListPage from "../erp/po/PoListPage";
import ReceiptCreatePage from "../erp/receipt/ReceiptCreatePage";
import ItemCreatePage from "../erp/item/ItemCreatePage";
import ItemListPage from "../erp/item/ItemListPage";
import VendorListPage from "../erp/vendor/VendorListPage";
import VendorCreatePage from "../erp/vendor/VendorCreatePage";
import StockListPage from "../erp/stock/StockListPage";
import StockUsageCreatePage from "../erp/stock/StockUsageCreatePage";
import StockUsageListPage from "../erp/stock/StockUsageListPage";
import StockUsageDetailPage from "../erp/stock/StockUsageDetailPage";


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
            { path: "/erp/item", element: <ItemListPage /> },
            { path: "/erp/item/new", element: <ItemCreatePage /> },
            { path: "/erp/item/:id", element: <ItemCreatePage /> },
            { path: "/erp/vendor", element: <VendorListPage /> },
            { path: "/erp/vendor/new", element: <VendorCreatePage /> },
            { path: "/erp/vendor/:code", element: <VendorCreatePage /> },
            { path: "/erp/stock", element: <StockListPage /> },
            { path: "/erp/stock-usage", element: <StockUsageListPage /> },
            { path: "/erp/stock-usage/new", element: <StockUsageCreatePage /> },
            { path: "/erp/stock-usage/:id", element: <StockUsageDetailPage /> },
            { path: "*", element: <Navigate to="/" replace /> },          // 안전망
        ],
    },
],
    { basename: "/erpProject" }
);
