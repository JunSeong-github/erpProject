import { useLocation, useNavigate, useParams } from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { CSSProperties, useState } from "react";
import {
    approveStockUsage,
    getStockUsageDetail,
    rejectStockUsage,
    StockUsage,
} from "../api";
import { useAuth } from "../../app/AuthContext";

const errMsg = (e: any) =>
    e?.response?.headers?.["x-error-detail"] ||
    e?.response?.data?.message ||
    e?.message ||
    "오류가 발생했습니다.";

export default function StockUsageDetailPage() {
    const { id } = useParams();
    const usageId = Number(id);
    const navigate = useNavigate();
    const location = useLocation();
    const queryClient = useQueryClient();
    const { user } = useAuth();
    const isAdmin = user?.role === "ADMIN";

    const statePage = location.state?.page ?? 0;
    const stateCondition = location.state?.searchCondition ?? {};

    const [working, setWorking] = useState(false);

    const { data: usage, isLoading, refetch } = useQuery<StockUsage>({
        queryKey: ["stockUsageDetail", usageId],
        queryFn: () => getStockUsageDetail(usageId),
        enabled: Number.isFinite(usageId),
    });

    const goBackToList = () => {
        const params = new URLSearchParams({ page: String(statePage) });
        if (stateCondition.itemName) params.append("itemName", stateCondition.itemName);
        if (stateCondition.status) params.append("status", stateCondition.status);
        navigate(`/erp/stock-usage?${params.toString()}`);
    };

    const invalidate = () => {
        queryClient.invalidateQueries({ queryKey: ["stockUsage"] });
        queryClient.invalidateQueries({ queryKey: ["stock"] });
    };

    const handleApprove = async () => {
        if (!window.confirm("이 재고 사용을 승인하시겠습니까? 승인 시 실재고에서 차감됩니다.")) return;
        setWorking(true);
        try {
            await approveStockUsage(usageId);
            alert("승인되었습니다.");
            invalidate();
            await refetch();
        } catch (e) {
            alert(errMsg(e));
        } finally {
            setWorking(false);
        }
    };

    const handleReject = async () => {
        const reason = window.prompt("반려 사유를 입력해 주세요.");
        if (reason == null) return; // 취소
        if (!reason.trim()) {
            alert("반려 사유는 필수입니다.");
            return;
        }
        setWorking(true);
        try {
            await rejectStockUsage(usageId, reason.trim());
            alert("반려되었습니다.");
            invalidate();
            await refetch();
        } catch (e) {
            alert(errMsg(e));
        } finally {
            setWorking(false);
        }
    };

    if (isLoading) return <div>로딩중...</div>;
    if (!usage) return <div>데이터 없음</div>;

    const isRequested = usage.status === "REQUESTED";

    const labelCell: CSSProperties = {
        border: "1px solid #ccc",
        padding: "8px",
        background: "#f8fafc",
        fontWeight: 600,
        width: 130,
    };
    const valueCell: CSSProperties = { border: "1px solid #ccc", padding: "8px" };

    return (
        <div>
            <h2>재고 사용 상세</h2>

            <table style={{ borderCollapse: "collapse", marginBottom: 16, minWidth: 480 }}>
                <tbody>
                <tr>
                    <td style={labelCell}>품목</td>
                    <td style={valueCell}>
                        {usage.itemName} ({usage.itemCode})
                    </td>
                </tr>
                <tr>
                    <td style={labelCell}>사용용도</td>
                    <td style={valueCell}>{usage.purpose}</td>
                </tr>
                <tr>
                    <td style={labelCell}>사용처</td>
                    <td style={valueCell}>{usage.usagePlace}</td>
                </tr>
                <tr>
                    <td style={labelCell}>사용량</td>
                    <td style={valueCell}>{usage.usageQty}</td>
                </tr>
                <tr>
                    <td style={labelCell}>사용일</td>
                    <td style={valueCell}>{usage.usageDate ?? ""}</td>
                </tr>
                <tr>
                    <td style={labelCell}>비고</td>
                    <td style={valueCell}>{usage.remark ?? ""}</td>
                </tr>
                <tr>
                    <td style={labelCell}>상태</td>
                    <td style={valueCell}>{usage.statusLabel}</td>
                </tr>
                {usage.status === "REJECTED" && (
                    <tr>
                        <td style={labelCell}>반려사유</td>
                        <td style={{ ...valueCell, color: "#dc2626" }}>{usage.rejectReason}</td>
                    </tr>
                )}
                <tr>
                    <td style={labelCell}>등록일</td>
                    <td style={valueCell}>{usage.createdDate?.slice(0, 10) ?? ""}</td>
                </tr>
                </tbody>
            </table>

            <div style={{ display: "flex", gap: 8 }}>
                <button type="button" onClick={goBackToList}>
                    목록으로
                </button>

                {isRequested && isAdmin && (
                    <>
                        <button type="button" onClick={handleApprove} disabled={working}>
                            {working ? "처리 중..." : "승인"}
                        </button>
                        <button type="button" onClick={handleReject} disabled={working}>
                            반려
                        </button>
                    </>
                )}
            </div>

            {!isRequested && (
                <div style={{ marginTop: 12, color: "#64748b" }}>
                    ※ 이미 {usage.statusLabel} 처리된 건은 승인/반려할 수 없습니다.
                </div>
            )}

            {isRequested && !isAdmin && (
                <div style={{ marginTop: 12, color: "#64748b" }}>
                    ※ 승인/반려는 관리자만 가능합니다.
                </div>
            )}
        </div>
    );
}
