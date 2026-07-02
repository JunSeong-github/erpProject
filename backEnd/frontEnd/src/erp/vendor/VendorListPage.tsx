import { useState } from "react";
import { Vendor, VendorSearchCondition, listVendor, PageResp } from "../api";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { keepPreviousData, useQuery } from "@tanstack/react-query";

export default function VendorListPage() {

    const [searchParams, setSearchParams] = useSearchParams();

    const initialPage = Math.max(Number(searchParams.get("page") ?? "0"), 0);
    const initialVendorName = searchParams.get("vendorName") || "";
    const initialVendorCode = searchParams.get("vendorCode") || "";

    const [page, setPage] = useState(initialPage);
    const pageSize = 10;

    const [searchCondition, setSearchCondition] = useState<VendorSearchCondition>({
        vendorName: initialVendorName,
        vendorCode: initialVendorCode,
    });

    const [appliedCondition, setAppliedCondition] = useState<VendorSearchCondition>({
        vendorName: initialVendorName,
        vendorCode: initialVendorCode,
    });

    const updateUrlParams = (newPage: number, condition: VendorSearchCondition) => {
        const params: Record<string, string> = {
            page: String(newPage)
        };

        if (condition.vendorName) params.vendorName = condition.vendorName;
        if (condition.vendorCode) params.vendorCode = condition.vendorCode;

        setSearchParams(params);
    };

    const { data, isLoading, isError, error, refetch } = useQuery<PageResp<Vendor>>({
        queryKey: ["vendor", page, appliedCondition],
        queryFn: () => listVendor({ page, size: pageSize, condition: appliedCondition }),
        placeholderData: keepPreviousData,
    });

    const vendorList = data?.content ?? [];
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

    return (
        <div>
            <h2>공급사 목록</h2>

            <div style={{ marginBottom: "10px" }}>
                <label>
                    &nbsp;공급사명:&nbsp;
                    <input
                        type="text"
                        style={{ width: "150px" }}
                        value={searchCondition.vendorName || ""}
                        onChange={(e) => setSearchCondition({
                            ...searchCondition,
                            vendorName: e.target.value
                        })}
                    />
                </label>

                <label>
                    &nbsp;공급사코드:&nbsp;
                    <input
                        type="text"
                        style={{ width: "150px" }}
                        value={searchCondition.vendorCode || ""}
                        onChange={(e) => setSearchCondition({
                            ...searchCondition,
                            vendorCode: e.target.value
                        })}
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

            <div style={{ marginBottom: "10px", display: "flex", gap: "8px" }}>
                <Link to="/erp/vendor/new">
                    <button type="button">새 공급사 추가</button>
                </Link>
                <Link to="/erp/vendor/bulk">
                    <button type="button">대량 공급사 업로드</button>
                </Link>
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
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>공급사코드</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>공급사명</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>관리</th>
                        </tr>
                        </thead>
                        <tbody>
                        {vendorList.length === 0 && (
                            <tr>
                                <td
                                    colSpan={4}
                                    style={{ border: "1px solid #ccc", padding: "4px", textAlign: "center" }}
                                >
                                    데이터가 없습니다.
                                </td>
                            </tr>
                        )}

                        {vendorList.map((vendor, index) => (
                            <tr key={vendor.vendorCode}>
                                <td style={{ border: "1px solid #ccc", padding: "4px", textAlign: "center" }}>
                                    {currentPage * pageSize + index + 1}
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                    {vendor.vendorCode}
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                    {vendor.vendorName}
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                    <button
                                        type="button"
                                        onClick={() => navigate(`/erp/vendor/${vendor.vendorCode}`,
                                            { state: { page, searchCondition: appliedCondition } })}
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
                            {totalPages === 0
                                ? "0 / 0"
                                : `${currentPage + 1} / ${totalPages}`}{" "}
                            페이지
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
