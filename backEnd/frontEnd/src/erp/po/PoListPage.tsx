
import {useEffect, useState} from "react";
import { useQuery, keepPreviousData, useMutation, useQueryClient  } from "@tanstack/react-query";
import { listPo, PageResp, Po, approvePo, PoSearchCondition, getPoStatuses, PoStatusOption } from "../api";
import { Link, useNavigate, useSearchParams } from "react-router-dom";

export default function PoListPage() {

    const [searchParams, setSearchParams] = useSearchParams();

    const initialPage = Math.max(Number(searchParams.get("page") ?? "0"), 0);

// page 상태
    const [page, setPage] = useState(initialPage);
    const pageSize = 10;

    const [poStatuses, setPoStatuses] = useState<PoStatusOption[]>([]);

    const [searchCondition, setSearchCondition] = useState<PoSearchCondition>({
        vendorName: "",
        vendorCode: "",
        deliveryDate: "",
        poStatus: ""
    });

    const [appliedCondition, setAppliedCondition] = useState<PoSearchCondition>({});

    useEffect(() => {
        getPoStatuses().then(setPoStatuses).catch(console.error);
    }, []);

    const setPageAndSync = (next: number) => {
        setPage(next);
        setSearchParams({ page: String(next) });
    };

    const { data, isLoading, isError, error, refetch } = useQuery<PageResp<Po>>({
        queryKey: ["po", page],
        queryFn: () => listPo({ page, size: pageSize, condition: appliedCondition }),
        placeholderData: keepPreviousData,
    });

    const queryClient = useQueryClient();

    const approveMutation = useMutation({
        mutationFn: (id: number) => approvePo(id),
        onSuccess: () => {
            // 목록 다시 불러오기
            queryClient.invalidateQueries({ queryKey: ["po"] });
        },
        onError: (err: unknown) => {
            alert((err as Error).message ?? "승인 중 오류 발생");
        },
    });

    const handleSearch = () => {

        setPage(0);
        setSearchParams({ page: "0" });
        setAppliedCondition(searchCondition);

        setTimeout(() => refetch(), 0);
    };

    const poList = data?.content ?? [];
    const currentPage = data?.number ?? 0;
    const totalPages = data?.totalPages ?? 0;

    const navigate = useNavigate();

    return (
        <div>
            <h2>발주 목록</h2>

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
                        style={{ width:"150px" }}
                        value={searchCondition.vendorCode || ""}
                        onChange={(e) => setSearchCondition({
                            ...searchCondition,
                            vendorCode: e.target.value
                        })}
                    />
                </label>

                <label>
                    &nbsp;납기요청일:&nbsp;
                    <input
                        type="date"
                        style={{ width: "150px" }}
                        value={searchCondition.deliveryDate || ""}
                        onChange={(e) => setSearchCondition({
                            ...searchCondition,
                            deliveryDate: e.target.value
                        })}
                    />
                </label>

                <label>
                    &nbsp;발주 상태:&nbsp;
                    <select
                        style={{ width: "150px" }}
                        value={searchCondition.poStatus || ""}
                        onChange={(e) => setSearchCondition({
                            ...searchCondition,
                            poStatus: e.target.value
                        })}
                    >
                        <option value="">전체</option>

                        {poStatuses.map((status) => (
                            <option key={status.code} value={status.code}>
                                {status.label}
                            </option>
                        ))}
                    </select>
                </label>

                <button
                    type="button"
                    onClick={handleSearch}  //
                    style={{ padding: "4px 8px", cursor: "pointer", marginLeft: "10px" }}
                >
                    조회
                </button>
            </div>

            <div style={{ marginBottom: "10px" }}>
                <Link to="/erp/po/new">
                    <button type="button">새 발주 작성</button>
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
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>공급사</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>납기요청일</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>상태</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>비고</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>생성일</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>관리</th>
                        </tr>
                        </thead>
                        <tbody>
                        {poList.length === 0 && (
                            <tr>
                                <td
                                    colSpan={6}
                                    style={{ border: "1px solid #ccc", padding: "4px", textAlign: "center" }}
                                >
                                    데이터가 없습니다.
                                </td>
                            </tr>
                        )}

                        {poList.map((po) => (
                            <tr key={po.id}>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                    {po.vendorName} ({po.vendorCode})
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                    {po.deliveryDate}
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                    {po.poStatusLabel}
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                    {po.etc}
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                    {po.createDate?.slice(0, 10)}
                                </td>
                                <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                    <button
                                        type="button"
                                        onClick={() => navigate(po.poStatus === "DRAFT" || po.poStatus === "REJECTED" || po.poStatus === "APPROVED" ? `/erp/po/${po.id}` : `/erp/receipt/${po.id}`, { state: { page } })}
                                        style={{ padding: "4px 8px", cursor: "pointer" }}
                                    >
                                        상세보기
                                    </button>

                                    {po.poStatus === "DRAFT" && (
                                        <button
                                            type="button"
                                            onClick={() => approveMutation.mutate(po.id)}
                                            disabled={approveMutation.isPending}
                                            style={{ padding: "4px 8px", cursor: "pointer" }}
                                        >
                                            {approveMutation.isPending ? "승인 중..." : "승인"}
                                        </button>
                                    )}
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