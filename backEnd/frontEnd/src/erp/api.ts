import { api } from "../lib/axios";
import type { AxiosError } from "axios";

// const baseUrl = import.meta.env.VITE_API_BASE ?? "https://erpproject-pu8e.onrender.com";
const baseUrl = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";
// const baseUrl = "https://erpproject-pu8e.onrender.com";

/** axios 에러에서 백엔드 메시지({code,message} 등)를 추출해 사용자용 Error 로 변환 */
function apiErr(e: unknown, fallback: string): Error {
    const ax = e as AxiosError<any>;
    const data = ax?.response?.data as any;
    const msg =
        (typeof data === "string" ? data : data?.message || data?.msg || data?.error) ||
        ax?.message ||
        fallback;
    return new Error(msg);
}

/** 인증 */
export type AuthUser = {
    loginId: string;
    username: string;
    role: string;       // ADMIN / EMPLOYEE
    roleLabel: string;  // 관리자 / 직원
};

export const login = (loginId: string, password: string) =>
    api.post<AuthUser>('/auth/login', { loginId, password }).then((r) => r.data);

export const logout = () =>
    api.post('/auth/logout').then((r) => r.data);

export const fetchMe = () =>
    api.get<AuthUser>('/auth/me').then((r) => r.data);
/** 인증 */

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

export interface PoSearchCondition {
    vendorName?: string;
    vendorCode?: string;
    deliveryDate?: string;
    poStatus?: string;
}

export type PoStatusOption = {
    code: string;
    label: string;
};

export async function getPoStatuses() {
    const res = await fetch(`${baseUrl}/po/statuses`);
    if (!res.ok) throw new Error("상태 목록 조회 실패");
    return (await res.json()) as PoStatusOption[];
}

// 목록 조회
export async function listPo(params: { page: number; size: number; condition?: PoSearchCondition; }) {
    const query = new URLSearchParams({
        page: String(params.page), // 0-base
        size: String(params.size),
        // sort: "deliveryDate,desc", // 정렬 넣고 싶으면 여기
    });

    if (params.condition) {
        if (params.condition.vendorName) {
            query.append("vendorName", params.condition.vendorName);
        }
        if (params.condition.vendorCode) {
            query.append("vendorCode", params.condition.vendorCode);
        }
        if (params.condition.deliveryDate) {
            query.append("deliveryDate", params.condition.deliveryDate);
        }
        if (params.condition.poStatus) {
            query.append("poStatus", params.condition.poStatus);
        }
    }

    console.log(baseUrl);
    const res = await fetch(`${baseUrl}/po/list?${query.toString()}`);
    if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "PO 목록 조회 실패");
    }

    return (await res.json()) as PageResp<Po>;
}

/** 발주 목록 조회 */

/** 발주 신규 등록 / 수정 payload (백엔드 PoCreateRequest 매핑) */
export type PoWriteRequest = {
    vendorCode: string;
    deliveryDate: string;
    etc?: string;
    poStatus?: string;
    lines: {
        itemId: number | string;
        quantity: number | string;
        unitPrice: number | string;
        amount: number | string;
    }[];
};

/** 발주 신규 등록 */
export async function createPo(payload: PoWriteRequest) {
    try {
        return (await api.post("/po/create", payload)).data;
    } catch (e) {
        throw apiErr(e, "발주 등록 실패");
    }
}

/** 발주 수정 */
export async function updatePo(id: number | string, payload: PoWriteRequest) {
    try {
        return (await api.put(`/po/${id}`, payload)).data;
    } catch (e) {
        throw apiErr(e, "발주 수정 실패");
    }
}

/** 발주 삭제 */
export async function deletePo(id: number | string) {
    try {
        return (await api.delete(`/po/${id}`)).data;
    } catch (e) {
        throw apiErr(e, "발주 삭제 실패");
    }
}

/** 발주 승인 */
export async function approvePo(id: number) {
    try {
        await api.post(`/po/${id}/approve`);
    } catch (e) {
        throw apiErr(e, "승인 실패");
    }
}
/** 발주 승인 */

/** 발주 반려 */

export async function rejectPo(poId: number, reason: string) {
    try {
        await api.post(`/po/${poId}/reject`, { reason });
    } catch (e) {
        throw apiErr(e, "반려 실패");
    }
}

/** 발주 반려 */

/** 발주 상세조회  */
export async function getDetail(id: number) {
    const res = await fetch(`${baseUrl}/po/${id}`);
    if (!res.ok) throw new Error("상세 조회 실패");
    return res.json();
}
/** 발주 상세조회 */

/** 입고진행 */
export async function startReceiving(poId: number) {
    try {
        await api.post(`/po/startReceiving/${poId}`);
    } catch (e) {
        throw apiErr(e, "입고진행 실패");
    }
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
    try {
        await api.post(`/receipt/create/${poId}`, req);
    } catch (e) {
        const ax = e as AxiosError<any>;
        // 백엔드가 상세 사유를 헤더로 줄 수 있음(대소문자 무관 → axios는 소문자 키)
        const headerDetail = ax.response?.headers?.["x-error-detail"];
        if (headerDetail) throw new Error(String(headerDetail));
        throw apiErr(e, `HTTP ${ax.response?.status ?? ""}`);
    }
}
/** 입고등록 */

/** 입고 등록내역 */

/** 대량 입고 엑셀 업로드 (2단계: 미리보기 → 확정) */

// 미리보기 한 행(백엔드 BulkReceiptPreviewResponse.PreviewRow 와 매핑)
export type BulkPreviewRow = {
    rowNo: number;
    poId: number | null;
    poItemId: number | null;
    itemCode: string | null;
    itemName: string | null;
    receivedQty: number | null;
    receiptDate: string | null;
    remark: string | null;
    lineRemark: string | null;
    valid: boolean;
    error: string | null;
};

export type BulkPreviewResponse = {
    totalRows: number;
    validRows: number;
    errorRows: number;
    confirmable: boolean;
    rows: BulkPreviewRow[];
};

export type BulkReceiptResult = {
    totalRows: number;
    successRows: number;
    failRows: number;
    createdReceipts: number;
    errors: { row: number; message: string }[];
};

/** 엑셀 업로드 → 파싱+검증만(저장 X). 행별 정상/오류를 돌려준다. */
export const previewReceiptBulk = (file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return api.post<BulkPreviewResponse>("/receipt/bulk/preview", fd).then((r) => r.data);
};

/** 미리보기에서 오류가 없을 때, 같은 파일을 확정 저장(batch insert). */
export const uploadReceiptBulk = (file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return api.post<BulkReceiptResult>("/receipt/bulk/upload", fd).then((r) => r.data);
};

/** 대량 입고 엑셀 업로드 */

/** 대량 발주 엑셀 업로드 (2단계: 미리보기 → 확정) */

export type BulkPoPreviewRow = {
    rowNo: number;
    groupLabel: string | null;
    vendorCode: string | null;
    vendorName: string | null;
    itemCode: string | null;
    itemName: string | null;
    quantity: number | null;
    unitPrice: number | null;
    amount: number | null;
    deliveryDate: string | null;
    etc: string | null;
    valid: boolean;
    error: string | null;
};

export type BulkPoPreviewResponse = {
    totalRows: number;
    validRows: number;
    errorRows: number;
    poCount: number;
    confirmable: boolean;
    rows: BulkPoPreviewRow[];
};

export type BulkPoResult = {
    totalRows: number;
    successRows: number;
    failRows: number;
    createdPos: number;
    errors: { row: number; message: string }[];
};

/** 엑셀 업로드 → 파싱+검증만(저장 X). 행별 정상/오류를 돌려준다. */
export const previewPoBulk = (file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return api.post<BulkPoPreviewResponse>("/po/bulk/preview", fd).then((r) => r.data);
};

/** 미리보기에서 오류가 없을 때, 같은 파일을 확정 저장(batch insert, DRAFT 생성). */
export const uploadPoBulk = (file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return api.post<BulkPoResult>("/po/bulk/upload", fd).then((r) => r.data);
};

/** 대량 발주 엑셀 업로드 */

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

/** 아이템 조회조건 */
export interface ItemSearchCondition {
    itemName?: string;
    itemCode?: string;
}
/** 아이템 조회조건 */

/** 대량 품목 엑셀 업로드 (미리보기 → 확정) */
export type BulkItemPreviewRow = {
    rowNo: number;
    itemCode: string | null;
    itemName: string | null;
    standardPrice: number | null;
    valid: boolean;
    error: string | null;
};
export type BulkItemPreviewResponse = {
    totalRows: number;
    validRows: number;
    errorRows: number;
    confirmable: boolean;
    rows: BulkItemPreviewRow[];
};
export type BulkItemResult = {
    totalRows: number;
    successRows: number;
    failRows: number;
    errors: { row: number; message: string }[];
};
export const previewItemBulk = (file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return api.post<BulkItemPreviewResponse>("/items/bulk/preview", fd).then((r) => r.data);
};
export const uploadItemBulk = (file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return api.post<BulkItemResult>("/items/bulk/upload", fd).then((r) => r.data);
};

/** 대량 공급사 엑셀 업로드 (미리보기 → 확정) */
export type BulkVendorPreviewRow = {
    rowNo: number;
    vendorCode: string | null;
    vendorName: string | null;
    valid: boolean;
    error: string | null;
};
export type BulkVendorPreviewResponse = {
    totalRows: number;
    validRows: number;
    errorRows: number;
    confirmable: boolean;
    rows: BulkVendorPreviewRow[];
};
export type BulkVendorResult = {
    totalRows: number;
    successRows: number;
    failRows: number;
    errors: { row: number; message: string }[];
};
export const previewVendorBulk = (file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return api.post<BulkVendorPreviewResponse>("/vendors/bulk/preview", fd).then((r) => r.data);
};
export const uploadVendorBulk = (file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return api.post<BulkVendorResult>("/vendors/bulk/upload", fd).then((r) => r.data);
};

/** 아이템 등록 */

export type ItemCreateRequest = {
    itemCode: string;
    itemName: string;
    standardPrice: number | string;
};

export const createItem = (data: ItemCreateRequest) =>
    api.post<Item>(`${baseUrl}/items/create`, data).then((r) => r.data);

export const updateItem = (id: Number, data: ItemCreateRequest) =>
    api.patch(`${baseUrl}/items/update/${id}`, data);

/** 아이템 등록 */

/** 아이템코드 중복체크 */

export const checkItemDuplicate = (itemCode: string) =>
    api.get<boolean>(`${baseUrl}/items/checkDuplicate`, { params: { itemCode } })
        .then((r) => r.data);

/** 아이템이름 중복체크 */
export const checkItemNameDuplicate = (itemName: string) =>
    api.get<boolean>(`${baseUrl}/items/checkDuplicateName`, { params: { itemName } })
        .then((r) => r.data);

/** 아이템코드 중복체크 */

/** 아이템 상세조회  */

export const getItemDetail = (id: number) =>
    api.get<Item>(`${baseUrl}/items/${id}`).then((r) => r.data);
/** 아이템 상세조회 */

/** 아이템 사용여부(발주/입고에 사용됐는지) — 사용된 품목은 수정·삭제 제한 */
export const getItemInUse = (id: number) =>
    api.get<boolean>(`${baseUrl}/items/${id}/in-use`).then((r) => r.data);

/** 아이템 삭제 */

export const deleteItem = (id: number) =>
    api.delete<Item>(`${baseUrl}/items/${id}`).then((r) => r.data);

/** 아이템 삭제 */

/** 아이템 조회 */

export const listItem = (params: {
    page: number;
    size: number;
    condition?: ItemSearchCondition;
}) => {
    const queryParams: any = {
        page: params.page,
        size: params.size,
    };

    if (params.condition?.itemName) {
        queryParams.itemName = params.condition.itemName;
    }
    if (params.condition?.itemCode) {
        queryParams.itemCode = params.condition.itemCode;
    }

    return api.get<PageResp<Item>>('/items/list', { params: queryParams })
        .then((r) => r.data);
};

/** 아이템 조회 */

/** 재고 */
export type Stock = {
    itemId: number;
    itemCode: string;
    itemName: string;
    standardPrice: number | string;
    stockQty: number;
};

/** 재고 목록조회(페이징) */
export const listStock = (params: {
    page: number;
    size: number;
    condition?: ItemSearchCondition;
}) => {
    const queryParams: any = {
        page: params.page,
        size: params.size,
    };

    if (params.condition?.itemName) queryParams.itemName = params.condition.itemName;
    if (params.condition?.itemCode) queryParams.itemCode = params.condition.itemCode;

    return api.get<PageResp<Stock>>('/items/stock', { params: queryParams })
        .then((r) => r.data);
};
/** 재고 조회 */

/** 재고 사용(출고 요청) */
export type UsageStatusOption = {
    code: string;
    label: string;
};

export const getUsageStatuses = () =>
    api.get<UsageStatusOption[]>('/stock-usage/statuses').then((r) => r.data);

export interface StockUsageSearchCondition {
    itemName?: string;
    status?: string;
}

export type StockUsage = {
    id: number;
    itemId: number;
    itemCode: string;
    itemName: string;
    purpose: string;
    usagePlace: string;
    usageQty: number;
    usageDate?: string;
    remark?: string;
    status: string;
    statusLabel: string;
    rejectReason?: string;
    createdDate?: string;
};

export type StockUsageCreateRequest = {
    itemId: number;
    purpose: string;
    usagePlace: string;
    usageQty: number;
    usageDate?: string;
    remark?: string;
};

/** 재고 사용 등록 */
export const createStockUsage = (data: StockUsageCreateRequest) =>
    api.post<number>('/stock-usage/create', data).then((r) => r.data);

/** 재고 사용 목록조회(페이징) */
export const listStockUsage = (params: {
    page: number;
    size: number;
    condition?: StockUsageSearchCondition;
}) => {
    const queryParams: any = {
        page: params.page,
        size: params.size,
    };

    if (params.condition?.itemName) queryParams.itemName = params.condition.itemName;
    if (params.condition?.status) queryParams.status = params.condition.status;

    return api.get<PageResp<StockUsage>>('/stock-usage/list', { params: queryParams })
        .then((r) => r.data);
};

/** 재고 사용 상세조회 */
export const getStockUsageDetail = (id: number) =>
    api.get<StockUsage>(`/stock-usage/${id}`).then((r) => r.data);

/** 재고 사용 승인 */
export const approveStockUsage = (id: number) =>
    api.post(`/stock-usage/${id}/approve`).then((r) => r.data);

/** 재고 사용 반려 */
export const rejectStockUsage = (id: number, reason: string) =>
    api.post(`/stock-usage/${id}/reject`, { reason }).then((r) => r.data);

/** 재고 사용 */


/** 공급사 조회조건 */
export interface VendorSearchCondition {
    vendorCode?: string;
    vendorName?: string;
}
/** 공급사 조회조건 */

/** 공급사 등록요청 */
export type VendorCreateRequest = {
    vendorCode: string;
    vendorName: string;
};

/** 공급사 등록 */
export const createVendor = (data: VendorCreateRequest) =>
    api.post<Vendor>(`${baseUrl}/vendors/create`, data).then((r) => r.data);

/** 공급사 수정 (공급사명) */
export const updateVendor = (vendorCode: string, data: VendorCreateRequest) =>
    api.patch(`${baseUrl}/vendors/update/${vendorCode}`, data);

/** 공급사코드 중복체크 */
export const checkVendorDuplicate = (vendorCode: string) =>
    api.get<boolean>(`${baseUrl}/vendors/checkDuplicate`, { params: { vendorCode } })
        .then((r) => r.data);

/** 공급사명 중복체크 */
export const checkVendorNameDuplicate = (vendorName: string) =>
    api.get<boolean>(`${baseUrl}/vendors/checkDuplicateName`, { params: { vendorName } })
        .then((r) => r.data);

/** 공급사 상세조회 */
export const getVendorDetail = (vendorCode: string) =>
    api.get<Vendor>(`${baseUrl}/vendors/${vendorCode}`).then((r) => r.data);

/** 공급사 사용여부(발주에 사용됐는지) — 사용된 공급사는 수정·삭제 제한 */
export const getVendorInUse = (vendorCode: string) =>
    api.get<boolean>(`${baseUrl}/vendors/${vendorCode}/in-use`).then((r) => r.data);

/** 공급사 삭제 */
export const deleteVendor = (vendorCode: string) =>
    api.delete(`${baseUrl}/vendors/${vendorCode}`).then((r) => r.data);

/** 공급사 목록조회(페이징) */
export const listVendor = (params: {
    page: number;
    size: number;
    condition?: VendorSearchCondition;
}) => {
    const queryParams: any = {
        page: params.page,
        size: params.size,
    };

    if (params.condition?.vendorName) queryParams.vendorName = params.condition.vendorName;
    if (params.condition?.vendorCode) queryParams.vendorCode = params.condition.vendorCode;

    return api.get<PageResp<Vendor>>('/vendors/list', { params: queryParams })
        .then((r) => r.data);
};
/** 공급사 조회 */
