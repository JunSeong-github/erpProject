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