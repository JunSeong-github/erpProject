import { useQuery, useMutation, useQueryClient} from "@tanstack/react-query";
// import axios from "axios";
import { useParams, useNavigate, useLocation } from "react-router-dom";

import { listItems, listVendors, Item, Vendor, getDetail, approvePo, rejectPo, startReceiving } from "../api";
import {useEffect, useState} from "react";


type PoLine = {
    itemId: number;
    unitPrice: number;
    quantity: number;
    amount: number;
};

export default function PoCreatePage() {
    // 아이템 목록 로딩
    const { data: items = []} = useQuery<Item[]>({
        queryKey: ["items"],
        queryFn: listItems,
        staleTime: 1000 * 60, // 1분 캐싱 (옵션)
    });

    // 공급사 목록 로딩
    const { data: vendors = []} = useQuery<Vendor[]>({
        queryKey: ["vendors"],
        queryFn: listVendors,
        staleTime: 1000 * 60, // 1분 캐싱 (옵션)
    });

    const baseUrl = import.meta.env.VITE_API_BASE ?? "https://erpproject-pu8e.onrender.com";

    const { id } = useParams();
    const isEdit = Boolean(id);
    const navigate = useNavigate();
    const location = useLocation();
    const fromPage = (location.state as any)?.page ?? 0;

    const goList = () => navigate(`/erp/po?page=${fromPage}`);

    const queryClient = useQueryClient();

    //객체의 state를 저장 및 세팅하는것 꼭 필요함 없으면 데이터를 읽지못함
    const [vendorCode, setVendorCode] = useState("");
    const [deliveryDate, setDeliveryDate] = useState("");
    const [etc, setEtc] = useState("");
    const [showRejectBox, setShowRejectBox] = useState(false);
    const [rejectReason, setRejectReason] = useState("");

    const [lines, setLines] = useState<PoLine[]>([]);

    const [isSaving, setIsSaving] = useState(false);

    const { data: poDetail, refetch: refetchPoDetail } = useQuery({
        queryKey: ["poDetail", id],
        queryFn: () => getDetail(Number(id)),
        enabled: isEdit,  // id 있을 때만 호출
    });

    const modified =
        poDetail?.deliveryDate && poDetail?.poStatus === "DRAFT";

    const isEditable =
        poDetail?.deliveryDate && poDetail?.poStatus != "DRAFT";

    const isDraft = poDetail?.poStatus === "DRAFT";
    const isRejected = poDetail?.poStatus === "REJECTED";
    const isApproved = poDetail?.poStatus === "APPROVED";
    const isReceivable =
        poDetail?.poStatus === "ORDERED" || poDetail?.poStatus === "PARTIAL_RECEIVED";

    useEffect(() => {
        if (!poDetail) return;

        setVendorCode(poDetail.vendorCode);
        setDeliveryDate(poDetail.deliveryDate);
        setEtc(poDetail.etc || "");
        setRejectReason(poDetail.rejectReason ?? "");

        // 라인 초기화
        setLines(
            poDetail.lines.map((l: any) => ({
                itemId: Number(l.itemId),
                unitPrice: Number(l.unitPrice),
                quantity: Number(l.quantity),
                amount: Number(l.amount),
            }))
        );

    }, [poDetail]);

    // 라인 추가 버튼
    const addLine = () => {
        setLines((prev) => [
            ...prev,
            { itemId: 0, unitPrice: 0, quantity: 0, amount: 0 },
        ]);
    };

    const removeLine = (index: number) => {
        setLines((prev) => prev.filter((_, i) => i !== index));
    };

    //  각 라인의 값 변경 처리
    const updateLine = (
        index: number,
        field: keyof PoLine,
        value: string
    ) => {
        const newLines = [...lines];
        const line = newLines[index];

        if (!line) return;

        if (field === "itemId") {
            // 품목 선택 → 단가 자동 입력
            line.itemId = Number(value);
            const selectedItem = items.find((i) => i.id === line.itemId);
            line.unitPrice = Number(selectedItem?.standardPrice ?? 0);
        } else if (field === "unitPrice") {
            line.unitPrice = Number(value) || 0;
        } else if (field === "quantity") {
            line.quantity = Number(value) || 0;
        }

        // 단가 또는 수량 변경 → 합계 자동 계산
        line.amount = Number(line.unitPrice) * Number(line.quantity);

        setLines(newLines);
    };

    const handleSave = async () => {
        if (!vendorCode) {
            alert("공급사를 선택하세요.");
            return;
        }

        if (!deliveryDate) {
            alert("납기 요청일을 입력하세요.");
            return;
        }

        if (lines.length === 0) {
            alert("최소 1개 이상의 품목 라인이 필요합니다.");
            return;
        }

        // 라인 중에 itemId, quantity 없는 게 있는지 체크
        const invalidLineIndex = lines.findIndex(
            (l) => !l.itemId || l.quantity <= 0
        );
        if (invalidLineIndex !== -1) {
            alert(`${invalidLineIndex + 1}번째 라인의 품목/수량을 확인하세요.`);
            return;
        }

        // 백엔드에 보낼 payload
        const payload = {
            vendorCode: vendorCode,
            deliveryDate: deliveryDate,
            etc: etc,
            lines: lines.map((line) => ({
                itemId: String(line.itemId),
                quantity: String(line.quantity),
                unitPrice: String(line.unitPrice),
                amount: String(line.amount),
            })),
        };

        try {
            setIsSaving(true);

            const url = isEdit
                ? `${baseUrl}/po/${id}`       // 수정
                : `${baseUrl}/po/create`;     // 신규등록

            const method = isEdit ? "PUT" : "POST";

            const res = await fetch(url, {
                method,
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload),
            });

            if (!res.ok) throw new Error(await res.text());
            alert(isEdit ? "수정되었습니다." : "저장되었습니다.");
            if(!isEdit) goList();

        } catch (e) {
            console.error(e);
            alert("오류 발생");
        } finally {
            setIsSaving(false);
        }
    };

    const handleDelete = async () => {
        if (!id) return;

        const ok = confirm("정말로 이 발주를 삭제할까요?");
        if (!ok) return;

        try {

            const res = await fetch(`${baseUrl}/po/${id}`, { method: "DELETE" });

            if (!res.ok) throw new Error(await res.text());

            alert("삭제되었습니다.");
            // 삭제 후 목록으로 이동 (원하는 경로로)
            goList();
        } catch (e) {
            console.error(e);
            alert("삭제 중 오류 발생");
        }
    };

    const approveMutation = useMutation({
        mutationFn: (poId: number) => approvePo(poId),
        onSuccess: async () => {
            alert("승인되었습니다.");
         // 상세 데이터 다시 불러오기
            await queryClient.invalidateQueries({ queryKey: ["poDetail", id] });
            // 목록도 최신화(목록에서 승인 상태 바로 보이게)
            await queryClient.invalidateQueries({ queryKey: ["po"] });
        },
        onError: (err: unknown) => {
            alert((err as Error).message ?? "승인 중 오류 발생");
        },
    });

    const handleReject = async () => {
        if (!id) return;

        if (!rejectReason.trim()) {
            alert("반려 사유를 입력해주세요.");
            return;
        }

        try {
            await rejectPo(Number(id), rejectReason);
            alert("반려 처리되었습니다.");
            goList();
        } catch (e) {
            console.error(e);
            alert("반려 처리 중 오류가 발생했습니다.");
        }
    };


    return (
        <div>
            <h2>{isEdit ? "발주 수정" : "발주 작성"}</h2>

            <div>
            <label>
                공급사정보 :&nbsp;
                <select style={{width:"100px"}}
                        value={vendorCode}
                        disabled={isEditable}
                        onChange={(e) => setVendorCode(e.target.value)}
                >
                    <option value="">선택</option>
                    {vendors.map((vendor) => (
                        <option key={vendor.vendorCode} value={vendor.vendorCode}>
                            {vendor.vendorName} ({vendor.vendorCode})
                        </option>
                    ))}
                </select>
            </label>

            <label>
                &nbsp;납기 요청일 :&nbsp;
                <input
                    type="date"
                    readOnly={isEditable}
                    value={deliveryDate}
                    onChange={(e) => setDeliveryDate(e.target.value)}
                />
            </label>

                <label>
                    &nbsp;비고:&nbsp;
                    <input
                        type="text"
                        readOnly={isEditable}
                        value={etc}
                        style={{ width: "80px" }}
                        onChange={(e) => setEtc(e.target.value)}
                    />
                </label>

            </div>

            {(!isEdit || modified) && (
            <div>
                <button type="button" onClick={addLine}>라인 추가</button>
            </div>
            )}
            <div>
                {lines.map((line, index) => (
                    <div key={index} style={{ marginTop: "10px" }}>
                        {/* 품목 선택 */}
                        <label>
                            품목 선택:&nbsp;
                            <select
                                style={{ width: "150px" }}
                                disabled={isEditable}
                                value={line.itemId}
                                onChange={(e) => updateLine(index, "itemId", e.target.value)}
                            >
                                <option value="">선택</option>
                                {items.map((item) => (
                                    <option key={item.id} value={item.id}>
                                        {item.itemName} ({item.standardPrice}원)
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label>
                            &nbsp;단가:&nbsp;
                            <input
                                type="number"
                                value={line.unitPrice}
                                readOnly
                                onChange={(e) => updateLine(index, "unitPrice", e.target.value)}
                                style={{ width: "100px" }}
                            />
                        </label>

                        <label>
                            &nbsp;수량:&nbsp;
                            <input
                                type="number"
                                readOnly={isEditable}
                                value={line.quantity === 0 ? "" : line.quantity}
                                onChange={(e) => updateLine(index, "quantity", e.target.value)}
                                style={{ width: "80px" }}
                            />
                        </label>

                        <label>
                            &nbsp;합계:&nbsp;
                            <input
                                type="number"
                                value={line.amount}
                                readOnly
                                style={{ width: "100px", background: "#eee" }}
                            />
                        </label>

                        {modified && (
                            <button
                                type="button"
                                onClick={() => removeLine(index)}
                                style={{ marginLeft: "8px" }}
                            >
                                라인 삭제
                            </button>
                        )}
                    </div>
                ))}
            </div>

            {isRejected && (
                <div style={{ marginTop: "16px", padding: "12px", border: "1px solid #f00" }}>
                    <div style={{ fontWeight: 700, marginBottom: "6px" }}>반려 사유</div>
                    <div style={{ whiteSpace: "pre-wrap" }}>
                        {poDetail?.rejectReason || "(사유 없음)"}
                    </div>
                    <div style={{ marginTop: "10px", fontWeight: 700 }}>
                        반려된 건임으로 확인 후 재 발주 작성 부탁드립니다
                    </div>
                </div>
            )}

            {(!isEdit || modified) && (
            <div>
                <button
                    type="button"
                    onClick={handleSave}
                >
                    {isSaving ? "저장 중..." : isEdit ? "수정" : "저장"}
                </button>
            </div>
            )}

            {modified && (
                <div style={{ marginTop: "12px" }}>
                    <button type="button" onClick={handleDelete}>
                        발주 삭제
                    </button>
                </div>
            )}

            {modified && (
                <div style={{ marginTop: "12px" }}>
                    <button
                        type="button"
                        onClick={() => approveMutation.mutate(Number(id))}
                        disabled={approveMutation.isPending}
                    >
                        {approveMutation.isPending ? "승인 중..." : "승인"}
                    </button>
                </div>
            )}

            {isEdit && isDraft && (
                <div style={{ marginTop: "12px" }}>
                    {!showRejectBox ? (
                        <button type="button" onClick={() => setShowRejectBox(true)}>
                            반려
                        </button>
                    ) : (
                        <div style={{ marginTop: "8px" }}>
                            <div style={{ marginBottom: "6px" }}>
          <textarea
              placeholder="반려 사유를 입력하세요"
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              rows={4}
              style={{ width: "100%" }}
          />
                            </div>
                            <button type="button" onClick={handleReject}>
                                반려 확정
                            </button>
                            <button
                                type="button"
                                onClick={() => {
                                    setShowRejectBox(false);
                                    setRejectReason("");
                                }}
                                style={{ marginLeft: "8px" }}
                            >
                                취소
                            </button>
                        </div>
                    )}
                </div>
            )}

            {isEdit && isApproved && (
                <div style={{ marginTop: 12 }}>
                    <button
                        type="button"
                        onClick={async () => {
                            try {
                                await startReceiving(Number(id)); // ✅ APPROVED -> ORDERED
                                //  상세 다시 불러와서 상태 반영
                                await refetchPoDetail();
                                await queryClient.invalidateQueries({ queryKey: ["poDetail", Number(id)] });
                                alert("입고 진행 상태로 변경되었습니다.");
                            } catch (e) {
                                console.error(e);
                                alert("입고 진행 처리 중 오류가 발생했습니다.");
                            }
                        }}
                    >
                        입고 진행
                    </button>
                </div>
            )}

            {isEdit && isReceivable && (
                <div style={{ marginTop: 12 }}>
                    <button
                        type="button"
                        onClick={() => navigate(`/erp/receipt/${id}`, { state: { page: fromPage } })}
                    >
                        입고 등록 페이지로 이동
                    </button>
                </div>
            )}

            <button
                type="button"
                onClick={() => navigate(`/erp/po?page=${fromPage}`)}
            >
                목록으로
            </button>

        </div>
    );
}
