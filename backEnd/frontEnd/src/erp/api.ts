import { api } from "../lib/axios";

// const baseUrl = import.meta.env.VITE_API_BASE_URL;
const baseUrl = "https://erpproject-pu8e.onrender.com";

export interface PageResp<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}

/** 아이템 */
export type Item = {
    id: number;      // 또는 string 이면 string으로 변경
    itemCode: string;
    itemName: string;
    standardPrice: number | string;
};

/** 아이템 목록 조회 */
export const listItems = () =>
    api.get<Item[]>("/items").then((r) => r.data);

/** 공급사 */
export type Vendor = {
    vendorCode: string;      // 또는 string 이면 string으로 변경
    vendorName: string;
};

/** 공급사 목록 조회 */
export const listVendors = () =>
    api.get<Vendor[]>("/vendors").then((r) => r.data);

/** 발주 목록 조회 */

export type Po = {
    id: number;
    vendorName: string;
    vendorCode: string;
    deliveryDate: string; // LocalDate → "yyyy-MM-dd"
    poStatus: string;
    poStatusLabel: string;  // "발주요청"
    etc: string;
    createDate: string;  // BaseTimeEntity에서 온 값
}

// 목록 조회
export async function listPo(params: { page: number; size: number }) {
    const query = new URLSearchParams({
        page: String(params.page), // 0-base
        size: String(params.size),
        // sort: "deliveryDate,desc", // 정렬 넣고 싶으면 여기
    });
    console.log(baseUrl);
    const res = await fetch(`${baseUrl}/po/list?${query.toString()}`);
    if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "PO 목록 조회 실패");
    }

    return (await res.json()) as PageResp<Po>;
}

/** 발주 목록 조회 */

/** 발주 승인 */
export async function approvePo(id: number) {
    const res = await fetch(`${baseUrl}/po/${id}/approve`, {
        method: "POST",
    });

    if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "승인 실패");
    }
}
/** 발주 승인 */

/** 발주 반려 */

export async function rejectPo(poId: number, reason: string){
    const res = await fetch(`${baseUrl}/po/${poId}/reject`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ reason: reason }),
    });
    if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "반려 실패");
    }
}

/** 발주 반려 */

/** 발주 상세조회  */
export async function getDetail(id: number) {
    const res = await fetch(`${baseUrl}/po/${id}`);
    if (!res.ok) throw new Error("상세 조회 실패");
    return res.json();
}
/** 발주 수정 */

/** 입고진행 */
export async function startReceiving(poId: number) {
    const res = await fetch(`${baseUrl}/po/startReceiving/${poId}`, { method: "POST" });
    if (!res.ok) throw new Error(await res.text());
}
/** 입고진행 */

/** 입고등록 */
export type ReceiptLineCreateRequest = {
    poItemId: number;
    receivedQty: number;
    lineRemark?: string;
};

export type ReceiptCreateRequest = {
    receiptDate?: string; // YYYY-MM-DD
    remark?: string;
    lines: ReceiptLineCreateRequest[];
};

export async function createReceipt(poId: number, req: ReceiptCreateRequest) {
    const res = await fetch(`${baseUrl}/receipt/create/${poId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(req),
    });
    // if (!res.ok) throw new Error(await res.text());
    if (!res.ok) {
        // 1) 헤더에 detail을 실어준 경우(네가 했던 방식)
        const headerDetail = res.headers.get("X-Error-Detail");
        if (headerDetail) throw new Error(headerDetail);

        // 2) JSON 에러 응답인 경우 (ErrorResponseDto 등)
        const ct = res.headers.get("content-type") || "";
        if (ct.includes("application/json")) {
            const body = await res.json().catch(() => null);
            const msg = body?.message || body?.msg || body?.error || JSON.stringify(body);
            throw new Error(msg || `HTTP ${res.status}`);
        }

        // 3) 텍스트 응답인 경우
        const text = await res.text().catch(() => "");
        throw new Error(text || `HTTP ${res.status}`);
    }
}
/** 입고등록 */

/** 입고 등록내역 */

export async function getReceiptSummary(poId: number) {
    const res = await fetch(`${baseUrl}/receipt/summary/${poId}`);
    if (!res.ok) throw new Error(await res.text());
    return res.json() as Promise<{
        remark?: string;
        receivedQtyMap?: Record<string, number>; // key가 문자열로 옴
        lineRemarkMap?: Record<string, string>;
    }>;
}

/** 입고 등록내역 */
