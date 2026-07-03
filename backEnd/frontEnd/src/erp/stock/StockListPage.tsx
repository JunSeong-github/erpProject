import { useState } from "react";
import { ItemSearchCondition, listStock, PageResp, Stock } from "../api";
import { useSearchParams } from "react-router-dom";
import { keepPreviousData, useQuery } from "@tanstack/react-query";

export default function StockListPage() {
    const [searchParams, setSearchParams] = useSearchParams();

    const initialPage = Math.max(Number(searchParams.get("page") ?? "0"), 0);
    const initialItemName = searchParams.get("itemName") || "";
    const initialItemCode = searchParams.get("itemCode") || "";

    const [page, setPage] = useState(initialPage);
    const pageSize = 10;

    const [searchCondition, setSearchCondition] = useState<ItemSearchCondition>({
        itemName: initialItemName,
        itemCode: initialItemCode,
    });

    const [appliedCondition, setAppliedCondition] = useState<ItemSearchCondition>({
        itemName: initialItemName,
        itemCode: initialItemCode,
    });

    const updateUrlParams = (newPage: number, condition: ItemSearchCondition) => {
        const params: Record<string, string> = {
            page: String(newPage),
        };

        if (condition.itemName) params.itemName = condition.itemName;
        if (condition.itemCode) params.itemCode = condition.itemCode;

        setSearchParams(params);
    };

    const { data, isLoading, isError, error, refetch } = useQuery<PageResp<Stock>>({
        queryKey: ["stock", page, appliedCondition],
        queryFn: () => listStock({ page, size: pageSize, condition: appliedCondition }),
        placeholderData: keepPreviousData,
    });

    const stockList = data?.content ?? [];
    const currentPage = data?.number ?? 0;
    const totalPages = data?.totalPages ?? 0;

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

    return (
        <div>
            <h2>재고 현황</h2>

            <div style={{ marginBottom: "10px" }}>
                <label>
                    &nbsp;품목코드:&nbsp;
                    <input
                        type="text"
                        style={{ width: "150px" }}
                        value={searchCondition.itemCode || ""}
                        onChange={(e) =>
                            setSearchCondition({
                                ...searchCondition,
                                itemCode: e.target.value,
                            })
                        }
                        onKeyDown={(e) => { if (e.key === "Enter") handleSearch(); }}
                    />
                </label>

                <label>
                    &nbsp;품목명:&nbsp;
                    <input
                        type="text"
                        style={{ width: "150px" }}
                        value={searchCondition.itemName || ""}
                        onChange={(e) =>
                            setSearchCondition({
                                ...searchCondition,
                                itemName: e.target.value,
                            })
                        }
                        onKeyDown={(e) => { if (e.key === "Enter") handleSearch(); }}
                    />
                </label>

                <button
                    type="button"
                    onClick={handleSearch}
                    style={{ padding: "4px 8px", cursor: "pointer", marginLeft: "10px" }}
                >
                    조회
                </button>
            </div>

            {isLoading && <div>로딩 중...</div>}
            {isError && <div>에러: {(error as Error)?.message}</div>}

            {!isLoading && !isError && (
                <>
                    <table
                        style={{
                            borderCollapse: "collapse",
                            width: "100%",
                        }}
                    >
                        <thead>
                        <tr>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>순번</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>품목코드</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>품목명</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>기준단가</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>현재 재고수량</th>
                        </tr>
                        </thead>
                        <tbody>
                        {stockList.length === 0 && (
                            <tr>
                                <td
                                    colSpan={5}
                                    style={{ border: "1px solid #ccc", padding: "4px", textAlign: "center" }}
                                >
                                    데이터가 없습니다.
                                </td>
                            </tr>
                        )}

                        {stockList.map((stock, index) => (
                            <tr key={stock.itemId}>
                                <td style={{ border: "1px solid #ccc", padding: "4px", textAlign: "center" }}>
                                    {currentPage * pageSize + index + 1}
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                    {stock.itemCode}
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                    {stock.itemName}
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px", textAlign: "right" }}>
                                    {stock.standardPrice}
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px", textAlign: "right" }}>
                                    {stock.stockQty}
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
