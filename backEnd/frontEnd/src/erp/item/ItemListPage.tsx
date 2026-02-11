import {useState} from "react";
import {Item, ItemSearchCondition, listItem, PageResp } from "../api";
import {Link, useNavigate, useSearchParams} from "react-router-dom";
import {keepPreviousData, useQuery} from "@tanstack/react-query";

export default function ItemListPage(){

    const [searchParams, setSearchParams] = useSearchParams();

    const initialPage = Math.max(Number(searchParams.get("page") ?? "0"), 0);
    const initialItemName = searchParams.get("itemName") || "";

    const [page, setPage] = useState(initialPage);
    const pageSize = 10;


    const [searchCondition, setSearchCondition] = useState<ItemSearchCondition>({
        itemName: initialItemName
    })

    const [appliedCondition, setAppliedCondition] = useState<ItemSearchCondition>({
        itemName: initialItemName
    })

    const updateUrlParams = (newPage: number, condition: ItemSearchCondition) => {
        const params: Record<string, string> = {
            page: String(newPage)
        };

        if (condition.itemName) params.itemName = condition.itemName;

        setSearchParams(params);
    };

    const { data, isLoading, isError, error, refetch } = useQuery<PageResp<Item>>({
        queryKey: ["item", page, appliedCondition],
        queryFn: () => listItem({ page, size: pageSize, condition: appliedCondition }),
        placeholderData: keepPreviousData,
    });

    const itemList = data?.content ?? [];
    const currentPage = data?.number ?? 0;
    const totalPages = data?.totalPages ?? 0;

    const navigate = useNavigate();

    const setPageAndSync = (next: number) => {
        setPage(next);
        updateUrlParams(next, appliedCondition);
    };

    const handleSearch=()=>{

        setPage(0);

        setAppliedCondition(searchCondition);
        updateUrlParams(0, searchCondition);
        setTimeout(() => refetch(), 0);
    }

    return(
        <div>
            <h2>품목 목록</h2>

            <div style={{marginBottom:"10px"}}>
                <label>
                    &nbsp;품목명:&nbsp;
                    <input
                        type="text"
                        style={{width:"150px"}}
                        value={searchCondition.itemName || ""}
                        onChange={(e)=> setSearchCondition({
                            ...searchCondition,
                            itemName:e.target.value
                        })}
                    />
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
                <Link to="/erp/item/new">
                    <button type="button">새 물품 추가</button>
                </Link>
            </div>

            {isLoading && <div>로딩 중...</div>}
            {isError && <div>에러: {(error as Error)?.message}</div>}

            {!isLoading && !isError && (
                <>
                <table
                    style={{
                        borderCollapse:"collapse",
                        width:"100%",
                    }}
                    >
                    <thead>

                        <tr>
                            <th style={{border:"1px solid #ccc", padding: "4px"}}>순번</th>
                            <th style={{border:"1px solid #ccc", padding: "4px"}}>품목코드</th>
                            <th style={{border:"1px solid #ccc", padding: "4px"}}>품목명</th>
                            <th style={{border:"1px solid #ccc", padding: "4px"}}>품목가격</th>
                            <th style={{ border: "1px solid #ccc", padding: "4px" }}>관리</th>
                        </tr>

                    </thead>
                    <tbody>
                    {itemList.length === 0 && (
                        <tr>
                            <td colSpan={4}
                                style={{ border: "1px solid #ccc", padding: "4px", textAlign: "center" }}
                            >
                                데이터가 없습니다.

                            </td>
                        </tr>
                    )}

                    {itemList.map((item,index)=>(
                        <tr key={item.id}>
                            <td style={{ border: "1px solid #ccc", padding: "4px", textAlign: "center" }}>
                                {currentPage * pageSize + index + 1}
                            </td>
                            <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                {item.itemCode}
                            </td>
                            <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                {item.itemName}
                            </td>
                            <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                {item.standardPrice}
                            </td>

                            <td style={{ border: "1px solid #ccc", padding: "4px" }}>
                                <button
                                    type="button"
                                    onClick={() => navigate(`/erp/item/${item.id}`,
                                        { state: { page,
                                                searchCondition: appliedCondition
                                            } })}
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
    )
}