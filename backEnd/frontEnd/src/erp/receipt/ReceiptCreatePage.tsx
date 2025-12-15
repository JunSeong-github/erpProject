import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { createReceipt, getDetail, getReceiptSummary } from "../api";

// 너 PoDetail 타입이 있으면 그걸 쓰고, 없으면 any로 시작해도 됨.
type ReceiptLineUI = {
    poItemId: number;
    itemName: string;
    orderedQty: number;
    unitPrice: number;
    amount: number;

    totalReceivedQty : number;

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

    // PO 상세 다시 조회해서 화면 구성
    const { data: poDetail, isLoading } = useQuery({
        queryKey: ["poDetail", poId],
        queryFn: () => getDetail(poId),
        enabled: Number.isFinite(poId),
    });

    const { data: summary } = useQuery({
        queryKey: ["receiptSummary", poId],
        queryFn: () => getReceiptSummary(poId),
        enabled: Number.isFinite(poId),
    });

    const [headerRemark, setHeaderRemark] = useState("");
    const [lines, setLines] = useState<ReceiptLineUI[]>([]);
    const [saving, setSaving] = useState(false);

    const isReceived = poDetail?.poStatus === "RECEIVED";

    // PO 상세가 로딩되면 라인 초기화
    useEffect(() => {
        if (!poDetail) return;

        if (summary?.remark != null) {
            setHeaderRemark(summary.remark);
        }

        const receivedMap = summary?.receivedQtyMap ?? {};
        const lineRemarkMap = summary?.lineRemarkMap ?? {};

        const init: ReceiptLineUI[] = (poDetail.lines ?? []).map((l: any) => {
            const ordered = Number(l.quantity ?? 0);

            const poItemId = Number(l.poItemId ?? l.id);

            const totalReceivedQty = Number(receivedMap[String(poItemId)] ?? 0);
            const remaining = Math.max(0, ordered - totalReceivedQty);

            return {
                poItemId,
                itemName: String(l.itemName ?? ""),
                etc: String(l.etc ?? ""),
                poStatusLabel: String(l.poStatusLabel ?? ""),
                orderedQty: ordered,
                unitPrice: Number(l.unitPrice ?? 0),
                amount: Number(l.amount ?? 0),

                totalReceivedQty:totalReceivedQty,

                receivedQty: 0,
                remainingQty: remaining,
                lineRemark: String(lineRemarkMap[String(poItemId)] ?? ""),
            };
        });

        setLines(init);
    }, [poDetail, summary]);

    const onChangeReceived = (poItemId: number, v: string) => {
        const qty = v === "" ? 0 : Math.max(0, Number(v));

        setLines((prev) =>
            prev.map((x) => {
                if (x.poItemId !== poItemId) return x;

                const remaining = Math.max(0, x.orderedQty - (x.totalReceivedQty + qty));
                return { ...x, receivedQty: qty, remainingQty: remaining };
            })
        );
    };

    const onChangeLineRemark = (poItemId: number, v: string) => {
        setLines((prev) =>
            prev.map((x) => (x.poItemId === poItemId ? { ...x, lineRemark: v } : x))
        );
    };

    const canSubmit = useMemo(() => {
        if (!poDetail) return false;

        return poDetail.poStatus != "RECEIVED" ;
    }, [poDetail]);

    const handleSubmit = async () => {
        if (!canSubmit) {
            alert("입고 등록이 가능한 상태가 아닙니다.");
            return;
        }

        const overLines = lines.filter(
            (l) => l.totalReceivedQty + l.receivedQty > l.orderedQty
        );

        if (overLines.length > 0) {
            const msg =
                overLines
                    .map(
                        (l) =>
                            `${l.itemName}: 요청 ${l.orderedQty}, 누적 ${l.totalReceivedQty}, 이번 ${l.receivedQty} (합 ${l.totalReceivedQty + l.receivedQty})`
                    )
                    .join("\n") + "\n\n초과 입고는 저장할 수 없습니다.";

            alert(msg);
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
                    orderedQty:l.orderedQty,
                    totalReceivedQty:l.totalReceivedQty,
                })),
            });

            alert("입고 등록이 완료되었습니다.");
            goList();
        } catch (e: any) {
            console.error(e);
            alert(e?.message ?? "저장 중 오류가 발생했습니다.");

        } finally {
            setSaving(false);
        }
    };

    if (isLoading) return <div>로딩중...</div>;
    if (!poDetail) return <div>데이터 없음</div>;

    return (
        <div>
            <h2>입고 등록</h2>

            <div style={{ marginBottom: 12 }}>
                <div>PO ID: {poDetail.id}</div>
                <div>공급사: {poDetail.vendorName} ({poDetail.vendorCode})</div>
                <div>납기요청일: {String(poDetail.deliveryDate ?? "")}</div>
                <div>상태: {poDetail.poStatusLabel}</div>
                <div>발주 비고: {poDetail.etc}</div>
            </div>

            <div style={{ marginBottom: 12 }}>
                <label>
                    입고 비고:&nbsp;
                    <input
                        type="text"
                        value={headerRemark}
                        readOnly={isReceived}
                        onChange={(e) => setHeaderRemark(e.target.value)}
                        disabled={!canSubmit}
                    />
                </label>
            </div>

            <table style={{ borderCollapse: "collapse", width: "100%" }}>
                <thead>
                <tr>
                    <th style={{ border: "1px solid #ccc", padding: 6 }}>품목</th>
                    <th style={{ border: "1px solid #ccc", padding: 6 }}>발주당시가격</th>
                    <th style={{ border: "1px solid #ccc", padding: 6 }}>요청수량</th>
                    <th style={{ border: "1px solid #ccc", padding: 6 }}>합계가격</th>
                    <th style={{ border: "1px solid #ccc", padding: 6 }}>누적입고수량</th>
                    {!isReceived &&
                    <th style={{ border: "1px solid #ccc", padding: 6 }}>실제입고수량</th>
                    }
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
                        <td style={{ border: "1px solid #ccc", padding: 6 }}>{l.totalReceivedQty}</td>
                        {!isReceived &&
                        <td style={{ border: "1px solid #ccc", padding: 6 }}>
                            <input
                                type="number"
                                value={l.receivedQty ===0 ? "" : l.receivedQty}
                                onChange={(e) => onChangeReceived(l.poItemId, e.target.value)}
                                disabled={!canSubmit}
                            />
                        </td>
                        }
                        <td style={{ border: "1px solid #ccc", padding: 6 }}>{l.remainingQty}</td>
                        <td style={{ border: "1px solid #ccc", padding: 6 }}>
                            <input
                                type="text"
                                value={l.lineRemark}
                                readOnly={isReceived}
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
                {canSubmit && (
                <button
                    type="button"
                    onClick={handleSubmit}
                    disabled={!canSubmit || saving}
                    style={{ marginLeft: 8 }}
                >
                    {saving ? "저장 중..." : "입고 등록"}
                </button>
                    )}
            </div>

            {!canSubmit && (
                <div style={{ marginTop: 12 }}>
                    ※ 현재 상태({poDetail.poStatus})에서는 입고 등록이 불가합니다.
                </div>
            )}
        </div>
    );
}
