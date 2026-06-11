import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import {
    getUsageStatuses,
    listStockUsage,
    PageResp,
    StockUsage,
    StockUsageSearchCondition,
    UsageStatusOption,
} from "../api";

export default function StockUsageListPage() {
    const [searchParams, setSearchParams] = useSearchParams();

    const initialPage = Math.max(Number(searchParams.get("page") ?? "0"), 0);
    const initialItemName = searchParams.get("itemName") || "";
    const initialStatus = searchParams.get("status") || "";

    const [page, setPage] = useState(initialPage);
    const pageSize = 10;

    const [statuses, setStatuses] = useState<UsageStatusOption[]>([]);

    const [searchCondition, setSearchCondition] = useState<StockUsageSearchCondition>({
        itemName: initialItemName,
        status: initialStatus,
    });
    const [appliedCondition, setAppliedCondition] = useState<StockUsageSearchCondition>({
        itemName: initialItemName,
        status: initialStatus,
    });

    useEffect(() => {
        getUsageStatuses().then(setStatuses).catch(console.error);
    }, []);

    const updateUrlParams = (newPage: number, condition: StockUsageSearchCondition) => {
        const params: Record<string, string> = { page: String(newPage) };
        if (condition.itemName) params.itemName = condition.itemName;
        if (condition.status) params.status = condition.status;
        setSearchParams(params);
    };

    const { data, isLoading, isError, error, refetch } = useQuery<PageResp<StockUsage>>({
        queryKey: ["stockUsage", page, appliedCondition],
        queryFn: () => listStockUsage({ page, size: pageSize, condition: appliedCondition }),
        placeholderData: keepPreviousData,
    });

    const list = data?.content ?? [];
    const currentPage = data?.number ?? 0;
    const totalPages = data?.totalPages ?? 0;

    const navigate = useNavigate();

    const setPageAndSync = (next: number) => {
        setPage(next);
        updateUrlParams(next, appliedCondition);
    };

    const handleSearch = () => {
        setPage(0);
        setAppliedCondition(searchCondition);
        updateUrlParams(0, searchCondition);
        setTimeout(() => refetch(), 0);
    };

    const statusColor = (code: string) =>
        code === "APPROVED" ? "#16a34a" : code === "REJECTED" ? "#dc2626" : "#475569";

    return (
        <div>
            <h2>재고 사용현황</h2>

            <div style={{ marginBottom: "10px" }}>
                <label>
                    &nbsp;품목명:&nbsp;
                    <input
                        type="text"
                        style={{ width: "150px" }}
                        value={searchCondition.itemName || ""}
                        onChange={(e) =>
                            setSearchCondition({ ...searchCondition, itemName: e.target.value })
                        }
                    />
                </label>

                <label>
                    &nbsp;상태:&nbsp;
                    <select
                        style={{ width: "120px" }}
                        value={searchCondition.status || ""}
                        onChange={(e) =>
                            setSearchCondition({ ...searchCondition, status: e.target.value })
                        }
                    >
                        <option value="">전체</option>
                        {statuses.map((s) => (
                            <option key={s.code} value={s.code}>
                                {s.label}
                            </option>
                        ))}
                    </select>
                </label>

                <button
                    type="button"
                    onClick={handleSearch}
                    style={{ padding: "4px 8px", cursor: "pointer", marginLeft: "10px" }}
                >
                    조회
                </button>
            </div>

            <div style={{ marginBottom: "10px" }}>
                <Link to="/erp/stock-usage/new">
                    <button type="button">새 재고 사용 등록</button>
                </Link>
            </div>

            {isLoading && <div>로딩 중...</div>}
            {isError && <div>에러: {(error as Error)?.message}</div>}

            {!isLoading && !isError && (
                <>
                    <table style={{ borderCollapse: "collapse", width: "100%" }}>
                        <thead>
                        <tr>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>순번</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>품목</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>사용용도</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>사용처</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>사용량</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>사용일</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>상태</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>관리</th>
                        </tr>
                        </thead>
                        <tbody>
                        {list.length === 0 && (
                            <tr>
                                <td
                                    colSpan={8}
                                    style={{ border: "1px solid #ccc", padding: "4px", textAlign: "center" }}
                                >
                                    데이터가 없습니다.
                                </td>
                            </tr>
                        )}

                        {list.map((u, index) => (
                            <tr key={u.id}>
                                <td style={{ border: "1px solid #ccc", padding: "4px", textAlign: "center" }}>
                                    {currentPage * pageSize + index + 1}
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                    {u.itemName} ({u.itemCode})
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>{u.purpose}</td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>{u.usagePlace}</td>
                                <td style={{ border: "1px solid #ccc", padding: "4px", textAlign: "right" }}>
                                    {u.usageQty}
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>{u.usageDate ?? ""}</td>
                                <td style={{ border: "1px solid #ccc", padding: "4px", color: statusColor(u.status), fontWeight: 600 }}>
                                    {u.statusLabel}
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                    <button
                                        type="button"
                                        onClick={() =>
                                            navigate(`/erp/stock-usage/${u.id}`, {
                                                state: { page, searchCondition: appliedCondition },
                                            })
                                        }
                                        style={{ padding: "4px 8px", cursor: "pointer" }}
                                    >
                                        상세보기
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>

                    {/* 페이징 */}
                    <div style={{ marginTop: "10px", display: "flex", gap: "8px", alignItems: "center" }}>
                        <button
                            type="button"
                            disabled={currentPage === 0}
                            onClick={() => setPageAndSync(Math.max(page - 1, 0))}
                        >
                            이전
                        </button>

                        <span>
              {totalPages === 0 ? "0 / 0" : `${currentPage + 1} / ${totalPages}`} 페이지
            </span>

                        <button
                            type="button"
                            onClick={() => {
                                const next = data ? Math.min(page + 1, data.totalPages - 1) : page;
                                setPageAndSync(next);
                            }}
                            disabled={data ? currentPage >= totalPages - 1 : true}
                        >
                            다음
                        </button>
                    </div>
                </>
            )}
        </div>
    );
}
