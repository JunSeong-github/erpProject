
import { useState } from "react";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { listPo, PageResp, Po } from "../api";
import { Link, useNavigate } from "react-router-dom";

export default function PoListPage() {
    // 화면에서 보이는 페이지는 1부터, 서버에는 0부터 보내기
    const [page, setPage] = useState(0);
    const pageSize = 10;

    const { data, isLoading, isError, error } = useQuery<PageResp<Po>>({
        queryKey: ["po", page],
        queryFn: () => listPo({ page, size: pageSize }),
        placeholderData: keepPreviousData,
    });

    const poList = data?.content ?? [];
    const currentPage = data?.number ?? 0;
    const totalPages = data?.totalPages ?? 0;

    const navigate = useNavigate();

    return (
        <div>
            <h2>발주 목록</h2>

            <div style={{ marginBottom: "10px" }}>
                <Link to="/erp/po/new">
                    <button type="button">새 발주 작성</button>
                </Link>
            </div>

            {isLoading && <div>로딩 중...</div>}
            {isError && <div>에러: {(error as Error)?.message}</div>}

            {!isLoading && !isError && (
                <>
                    {/* 테이블 */}
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
                                    colSpan={5}
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
                                    {po.poStatus}
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
                                        onClick={() => navigate(`/erp/po/${po.id}`)}
                                        style={{ padding: "4px 8px", cursor: "pointer" }}
                                    >
                                        상세보기
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>

                    {/* 페이징 영역 */}
                    <div style={{ marginTop: "10px", display: "flex", gap: "8px", alignItems: "center" }}>
                        <button
                            type="button"
                            onClick={() => setPage((p) => Math.max(p - 1, 0))}
                            disabled={currentPage === 0}
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
                            onClick={() =>
                                setPage((p) =>
                                    data ? Math.min(p + 1, data.totalPages - 1) : p
                                )
                            }
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