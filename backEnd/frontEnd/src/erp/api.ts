import { api } from "../lib/axios";

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

    const baseUrl = import.meta.env.VITE_API_BASE;
    const res = await fetch(`${baseUrl}/po/list?${query.toString()}`);
    if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "PO 목록 조회 실패");
    }

    return (await res.json()) as PageResp<Po>;
}

/** 발주 목록 조회 */

