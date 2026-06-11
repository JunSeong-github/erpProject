import { CSSProperties, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { createStockUsage, listStock, PageResp, Stock } from "../api";

const errMsg = (e: any) =>
    e?.response?.headers?.["x-error-detail"] ||
    e?.response?.data?.message ||
    e?.message ||
    "오류가 발생했습니다.";

export default function StockUsageCreatePage() {
    const navigate = useNavigate();

    // 품목 + 현재고 목록 (드롭다운 용)
    const { data: stockData } = useQuery<PageResp<Stock>>({
        queryKey: ["stockAll"],
        queryFn: () => listStock({ page: 0, size: 1000 }),
    });
    const stockList = stockData?.content ?? [];

    const [itemId, setItemId] = useState<number | "">("");
    const [purpose, setPurpose] = useState("");
    const [usagePlace, setUsagePlace] = useState("");
    const [usageQty, setUsageQty] = useState("");
    const [usageDate, setUsageDate] = useState(new Date().toISOString().slice(0, 10));
    const [remark, setRemark] = useState("");
    const [saving, setSaving] = useState(false);

    const selectedStock = useMemo(
        () => stockList.find((s) => s.itemId === Number(itemId)),
        [stockList, itemId]
    );
    const currentStock = selectedStock?.stockQty ?? 0;

    const handleSubmit = async () => {
        if (itemId === "") {
            alert("품목을 선택해 주세요.");
            return;
        }
        const qty = Number(usageQty);
        if (!qty || qty <= 0) {
            alert("사용량은 1 이상이어야 합니다.");
            return;
        }
        if (!purpose.trim()) {
            alert("사용용도를 입력해 주세요.");
            return;
        }
        if (!usagePlace.trim()) {
            alert("사용처를 입력해 주세요.");
            return;
        }
        if (qty > currentStock) {
            alert(`현재 재고(${currentStock})보다 많이 사용할 수 없습니다.`);
            return;
        }

        setSaving(true);
        try {
            await createStockUsage({
                itemId: Number(itemId),
                purpose: purpose.trim(),
                usagePlace: usagePlace.trim(),
                usageQty: qty,
                usageDate,
                remark: remark.trim(),
            });
            alert("재고 사용 요청이 등록되었습니다.");
            navigate("/erp/stock-usage");
        } catch (e) {
            alert(errMsg(e));
        } finally {
            setSaving(false);
        }
    };

    const cell: CSSProperties = { padding: "8px 6px", verticalAlign: "middle" };
    const labelCell: CSSProperties = { ...cell, width: 120, fontWeight: 600 };

    return (
        <div>
            <h2>재고 사용 등록</h2>

            <table style={{ borderCollapse: "collapse", marginBottom: 12 }}>
                <tbody>
                <tr>
                    <td style={labelCell}>품목</td>
                    <td style={cell}>
                        <select
                            value={itemId}
                            onChange={(e) => setItemId(e.target.value === "" ? "" : Number(e.target.value))}
                            style={{ width: 260 }}
                        >
                            <option value="">선택</option>
                            {stockList.map((s) => (
                                <option key={s.itemId} value={s.itemId}>
                                    {s.itemName} ({s.itemCode}) - 현재고 {s.stockQty}
                                </option>
                            ))}
                        </select>
                        {itemId !== "" && (
                            <span style={{ marginLeft: 10, color: currentStock > 0 ? "#0f172a" : "#dc2626" }}>
                                현재 재고: <b>{currentStock}</b>
                            </span>
                        )}
                    </td>
                </tr>
                <tr>
                    <td style={labelCell}>사용용도</td>
                    <td style={cell}>
                        <input
                            type="text"
                            value={purpose}
                            onChange={(e) => setPurpose(e.target.value)}
                            style={{ width: 260 }}
                            placeholder="예) 생산 투입, 샘플 제작"
                        />
                    </td>
                </tr>
                <tr>
                    <td style={labelCell}>사용처</td>
                    <td style={cell}>
                        <input
                            type="text"
                            value={usagePlace}
                            onChange={(e) => setUsagePlace(e.target.value)}
                            style={{ width: 260 }}
                            placeholder="예) 1공장 / 영업팀"
                        />
                    </td>
                </tr>
                <tr>
                    <td style={labelCell}>사용량</td>
                    <td style={cell}>
                        <input
                            type="number"
                            value={usageQty}
                            onChange={(e) => setUsageQty(e.target.value)}
                            style={{ width: 120 }}
                            min={1}
                        />
                    </td>
                </tr>
                <tr>
                    <td style={labelCell}>사용일</td>
                    <td style={cell}>
                        <input
                            type="date"
                            value={usageDate}
                            onChange={(e) => setUsageDate(e.target.value)}
                        />
                    </td>
                </tr>
                <tr>
                    <td style={labelCell}>비고</td>
                    <td style={cell}>
                        <input
                            type="text"
                            value={remark}
                            onChange={(e) => setRemark(e.target.value)}
                            style={{ width: 360 }}
                        />
                    </td>
                </tr>
                </tbody>
            </table>

            <div>
                <button type="button" onClick={() => navigate("/erp/stock-usage")}>
                    목록으로
                </button>
                <button
                    type="button"
                    onClick={handleSubmit}
                    disabled={saving}
                    style={{ marginLeft: 8 }}
                >
                    {saving ? "저장 중..." : "등록"}
                </button>
            </div>
        </div>
    );
}
