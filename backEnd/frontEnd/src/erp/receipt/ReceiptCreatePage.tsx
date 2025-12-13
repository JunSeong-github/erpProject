import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { createReceipt, getDetail } from "../api";

// 너 PoDetail 타입이 있으면 그걸 쓰고, 없으면 any로 시작해도 됨.
type ReceiptLineUI = {
    poItemId: number;
    itemName: string;
    orderedQty: number;
    unitPrice: number;
    amount: number;

    receivedQty: number; // 입력
    remainingQty: number; // 자동
    lineRemark: string; // 입력
};

export default function ReceiptCreatePage() {
    const { id } = useParams();
    const poId = Number(id);

    const navigate = useNavigate();
    const location = useLocation();
    const fromPage = (location.state as any)?.page ?? 0;

    const goList = () => navigate(`/erp/po?page=${fromPage}`);

    // ✅ PO 상세 다시 조회해서 화면 구성 (읽기전용)
    const { data: poDetail, isLoading } = useQuery({
        queryKey: ["poDetail", poId],
        queryFn: () => getDetail(poId),
        enabled: Number.isFinite(poId),
    });

    const [headerRemark, setHeaderRemark] = useState("");
    const [lines, setLines] = useState<ReceiptLineUI[]>([]);
    const [saving, setSaving] = useState(false);

    // ✅ PO 상세가 로딩되면 라인 초기화
    useEffect(() => {
        if (!poDetail) return;

        // poDetail.lines 구조는 너 응답에 맞춰 매핑해야 함.
        // 아래는 "poItemId, itemName, quantity, unitPrice, amount"가 내려온다고 가정
        const init: ReceiptLineUI[] = (poDetail.lines ?? []).map((l: any) => {
            const ordered = Number(l.quantity ?? 0);
            return {
                poItemId: Number(l.poItemId),       // ⭐ 꼭 poItemId 내려와야 함
                itemName: String(l.itemName ?? ""),
                etc: String(l.etc ?? ""),
                poStatusLabel: String(l.poStatusLabel ?? ""),
                orderedQty: ordered,
                unitPrice: Number(l.unitPrice ?? 0),
                amount: Number(l.amount ?? 0),

                receivedQty: 0,
                remainingQty: ordered,
                lineRemark: "",
            };
        });

        setLines(init);
    }, [poDetail]);

    const onChangeReceived = (poItemId: number, v: string) => {
        const qty = v === "" ? 0 : Math.max(0, Number(v));
        setLines((prev) =>
            prev.map((x) =>
                x.poItemId === poItemId
                    ? { ...x, receivedQty: qty, remainingQty: Math.max(0, x.orderedQty - qty) }
                    : x
            )
        );
    };

    const onChangeLineRemark = (poItemId: number, v: string) => {
        setLines((prev) =>
            prev.map((x) => (x.poItemId === poItemId ? { ...x, lineRemark: v } : x))
        );
    };

    const canSubmit = useMemo(() => {
        if (!poDetail) return false;
        // ORDERED 또는 PARTIAL_RECEIVED에서만 입고등록 가능하게(백엔드도 동일하게 막는 게 정석)
        return poDetail.poStatus != "CANCELLED" ;
    }, [poDetail]);

    const handleSubmit = async () => {
        if (!canSubmit) {
            alert("입고 등록이 가능한 상태가 아닙니다.");
            return;
        }

        setSaving(true);
        try {
            const today = new Date().toISOString().slice(0, 10);

            await createReceipt(poId, {
                receiptDate: today,
                remark: headerRemark,
                lines: lines.map((l) => ({
                    poItemId: l.poItemId,
                    receivedQty: l.receivedQty,
                    lineRemark: l.lineRemark,
                })),
            });

            alert("입고 등록이 완료되었습니다.");
            goList();
        } catch (e) {
            console.error(e);
            alert("입고 등록 중 오류가 발생했습니다.");
        } finally {
            setSaving(false);
        }
    };

    if (isLoading) return <div>로딩중...</div>;
    if (!poDetail) return <div>데이터 없음</div>;

    return (
        <div>
            <h2>입고 등록</h2>

            {/* ✅ 발주 헤더 (읽기 전용 표시) */}
            <div style={{ marginBottom: 12 }}>
                <div>PO ID: {poDetail.id}</div>
                <div>공급사: {poDetail.vendorName} ({poDetail.vendorCode})</div>
                <div>납기요청일: {String(poDetail.deliveryDate ?? "")}</div>
                <div>상태: {poDetail.poStatusLabel}</div>
                <div>발주 비고: {poDetail.etc}</div>
            </div>

            {/* ✅ 입고 헤더 비고 */}
            <div style={{ marginBottom: 12 }}>
                <label>
                    입고 비고:&nbsp;
                    <input
                        type="text"
                        value={headerRemark}
                        onChange={(e) => setHeaderRemark(e.target.value)}
                        disabled={!canSubmit}
                    />
                </label>
            </div>

            {/* ✅ 라인 테이블 */}
            <table style={{ borderCollapse: "collapse", width: "100%" }}>
                <thead>
                <tr>
                    <th style={{ border: "1px solid #ccc", padding: 6 }}>품목</th>
                    <th style={{ border: "1px solid #ccc", padding: 6 }}>발주당시가격</th>
                    <th style={{ border: "1px solid #ccc", padding: 6 }}>요청수량</th>
                    <th style={{ border: "1px solid #ccc", padding: 6 }}>합계가격</th>
                    <th style={{ border: "1px solid #ccc", padding: 6 }}>실제입고수량</th>
                    <th style={{ border: "1px solid #ccc", padding: 6 }}>잔량</th>
                    <th style={{ border: "1px solid #ccc", padding: 6 }}>라인비고</th>
                </tr>
                </thead>
                <tbody>
                {lines.map((l) => (
                    <tr key={l.poItemId}>
                        <td style={{ border: "1px solid #ccc", padding: 6 }}>{l.itemName}</td>
                        <td style={{ border: "1px solid #ccc", padding: 6 }}>{l.unitPrice}</td>
                        <td style={{ border: "1px solid #ccc", padding: 6 }}>{l.orderedQty}</td>
                        <td style={{ border: "1px solid #ccc", padding: 6 }}>{l.amount}</td>
                        <td style={{ border: "1px solid #ccc", padding: 6 }}>
                            <input
                                type="number"
                                value={l.receivedQty ===0 ? "" : l.receivedQty}
                                onChange={(e) => onChangeReceived(l.poItemId, e.target.value)}
                                disabled={!canSubmit}
                            />
                        </td>
                        <td style={{ border: "1px solid #ccc", padding: 6 }}>{l.remainingQty}</td>
                        <td style={{ border: "1px solid #ccc", padding: 6 }}>
                            <input
                                type="text"
                                value={l.lineRemark}
                                onChange={(e) => onChangeLineRemark(l.poItemId, e.target.value)}

                            />
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>

            {/*<div>poStatus: {String(poDetail.poStatus)}</div>*/}
            {/*<div>poStatusLabel: {String(poDetail.poStatusLabel)}</div>*/}

            <div style={{ marginTop: 12 }}>
                <button type="button" onClick={goList}>
                    목록으로
                </button>

                <button
                    type="button"
                    onClick={handleSubmit}
                    disabled={!canSubmit || saving}
                    style={{ marginLeft: 8 }}
                >
                    {saving ? "저장 중..." : "입고 등록"}
                </button>
            </div>

            {!canSubmit && (
                <div style={{ marginTop: 12 }}>
                    ※ 현재 상태({poDetail.poStatus})에서는 입고 등록이 불가합니다.
                </div>
            )}
        </div>
    );
}
