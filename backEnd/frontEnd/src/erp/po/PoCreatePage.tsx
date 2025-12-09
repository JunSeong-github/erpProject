import { useQuery } from "@tanstack/react-query";
// import axios from "axios";

import { listItems, listVendors, Item, Vendor } from "../api";
import {useState} from "react";

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

    //객체의 state를 저장 및 세팅하는것 꼭 필요함 없으면 데이터를 읽지못함
    const [vendorCode, setVendorCode] = useState("");
    const [deliveryDate, setDeliveryDate] = useState("");
    const [etc, setEtc] = useState("");

    const [lines, setLines] = useState<PoLine[]>([]);

    const [isSaving, setIsSaving] = useState(false);

    // 라인 추가 버튼
    const addLine = () => {
        setLines((prev) => [
            ...prev,
            { itemId: 0, unitPrice: 0, quantity: 0, amount: 0 },
        ]);
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
        // 1) 간단한 유효성 체크
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

        // 2) 백엔드에 보낼 payload 만들기
        const payload = {
            // 이 이름들은 너 백엔드 DTO에 맞게 수정해줘
            vendorCode: vendorCode,      // 또는 bpCode, bpName 등
            deliveryDate: deliveryDate,  // "yyyy-MM-dd" -> LocalDate로 자동 매핑됨
            etc: etc,
            lines: lines.map((line) => ({
                itemId: String(line.itemId),     // 백엔드가 String이면 이렇게
                qty: String(line.quantity),      // POLineRequest.qty 가 String이면
                unitPrice: String(line.unitPrice),
                // total은 서버에서 다시 계산 가능하니까 굳이 안 보내도 됨
            })),
        };

        try {
            setIsSaving(true);
            //주소쪽 수정하고 save 어떻게 되는지 보기 
            const res = await fetch("/api/po", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(payload),
            });

            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || "저장 실패");
            }

            alert("저장되었습니다.");

            // 3) 저장 성공 후 폼 초기화 (원하면)
            // setVendorCode("");
            // setDeliveryDate("");
            // setEtc("");
            // setLines([]);
        } catch (e) {
            console.error(e);
            alert("저장 중 오류가 발생했습니다.");
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <div>
            <h2>발주 작성</h2>

            <div>
            <label>
                공급사정보 :&nbsp;
                <select style={{width:"100px"}}
                        value={vendorCode}
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
                    value={deliveryDate}
                    onChange={(e) => setDeliveryDate(e.target.value)}
                />
            </label>

                <label>
                    &nbsp;비고:&nbsp;
                    <input
                        type="text"
                        value={etc}
                        style={{ width: "80px" }}
                        onChange={(e) => setEtc(e.target.value)}
                    />
                </label>

            </div>

            <div>
                <button type="button" onClick={addLine}>라인 추가</button>
            </div>
            {/* 🔹 라인 반복 렌더링 */}
            <div>
                {lines.map((line, index) => (
                    <div key={index} style={{ marginTop: "10px" }}>
                        {/* 품목 선택 */}
                        <label>
                            품목 선택:&nbsp;
                            <select
                                style={{ width: "150px" }}
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

                        {/* 단가 */}
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

                        {/* 수량 */}
                        <label>
                            &nbsp;수량:&nbsp;
                            <input
                                type="number"
                                value={line.quantity === 0 ? "" : line.quantity}
                                onChange={(e) => updateLine(index, "quantity", e.target.value)}
                                style={{ width: "80px" }}
                            />
                        </label>

                        {/* 합계 */}
                        <label>
                            &nbsp;합계:&nbsp;
                            <input
                                type="number"
                                value={line.amount}
                                readOnly
                                style={{ width: "100px", background: "#eee" }}
                            />
                        </label>
                    </div>
                ))}
            </div>

            <div>
                <button
                    type="button"
                    onClick={handleSave}
                    disabled={isSaving}
                >
                    {isSaving ? "저장 중..." : "저장"}
                </button>
            </div>

        </div>
    );
}
